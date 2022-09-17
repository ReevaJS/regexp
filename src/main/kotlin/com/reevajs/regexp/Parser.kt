package com.reevajs.regexp

class Parser(private val codePoints: IntArray, private val unicode: Boolean) {
    private var nextGroupIndex = 0
    private var cursor = 0
    private val groupNames = mutableSetOf<String>()
    private val backrefNames = mutableSetOf<String>()
    private val indexedBackrefs = mutableListOf<Pair<ParserState.Mark, NumberInfo>>()

    private val states = mutableListOf<ParserState>()
    private val state: ParserState
        get() = states.last()

    private val codePoint: Int
        get() = codePoints[cursor]

    private val done: Boolean
        get() = cursor > codePoints.lastIndex

    constructor(source: String, unicode: Boolean) : this(source.codePoints().toArray(), unicode)

    fun parse(): Array<Opcode> {
        states.add(ParserState())

        // The entire match is implicitly group 0
        +StartGroupOp(nextGroupIndex++, name = null)
        state.alternationMark = state.mark()

        while (!done)
            parseSingle()

        expect(states.size == 1)

        +EndGroupOp
        +MatchOp
        
        // Ensure no unknown group names are used
        for (name in backrefNames) {
            if (name !in groupNames) 
                error("Unknown capturing group name \"$name\"")
        }

        // We need to make sure that all indexed backrefs exists. If not, they need to be
        // reinterpreted as octal escapes
        for ((mark, backref) in indexedBackrefs) {
            if (backref.value >= nextGroupIndex) {
                // Octal escape
                val octalCodePoints = backref.codePoints.takeWhile { it in 0x30..0x37 /* 0-7 */ }.toIntArray()
                mark.replace(CharOp(octalCodePoints.codePointsToString().toInt(radix = 8)))

                // Check to see if there are any remaining non-octal digits
                if (octalCodePoints.size != backref.codePoints.size) {
                    val nonOctalCodePoints = backref.codePoints.drop(octalCodePoints.size)
                    nonOctalCodePoints.asReversed().forEach {
                        mark.insertAfter(CharOp(it))
                    }
                }
            }
        }

        return state.build()
    }

    private fun parseSingle() {
        parsePrimary()

        if (!done)
            parseSecondary()
    }

    private fun parsePrimary() {
        when (codePoint) {
            0x28 /* ( */ -> {
                cursor++

                state.modifierMark = state.mark()
                states.add(ParserState())

                // TODO: This is very ugly
                var lookOp: ((Array<Opcode>) -> LookOp)? = null

                when {
                    consumeIf(0x3f, 0x3a /* ?: */) -> +StartGroupOp(index = null, name = null)
                    consumeIf(0x3f, 0x3d /* ?= */) -> lookOp = ::PositiveLookaheadOp
                    consumeIf(0x3f, 0x21 /* ?! */) -> lookOp = ::NegativeLookaheadOp
                    consumeIf(0x3f, 0x3c, 0x3d /* ?<= */) -> lookOp = ::PositiveLookbehindOp
                    consumeIf(0x3f, 0x3c, 0x21 /* ?<! */) -> lookOp = ::NegativeLookbehindOp
                    consumeIf(0x3f, 0x3c /* ?< */) -> {
                        val name = parseName(endDelimiter = 0x3e /* > */)
                        if (!groupNames.add(name))
                            error("Duplicate capturing group name \"$name\"")

                        +StartGroupOp(nextGroupIndex++, name)
                    }
                    else -> +StartGroupOp(nextGroupIndex++, name = null)
                }

                state.alternationMark = state.mark()

                while (!done && codePoint != 0x29 /* ) */)
                    parseSingle()

                if (!consumeIf(0x29 /* ) */))
                    error("Expected ')'")

                if (lookOp != null)
                    +MatchOp

                val groupState = states.removeLast()

                if (lookOp != null) {
                    +lookOp(groupState.build())
                } else {
                    state.merge(groupState)
                    +EndGroupOp
                }
            }
            0x5b /* [ */ -> {
                cursor++
                state.modifierMark = state.mark()

                val charClassOp = if (codePoint == 0x5e /* ^ */) {
                    cursor++
                    ::InvertedCharClassOp
                } else ::CharClassOp

                states.add(ParserState())
                var numTests = 0

                while (!done && codePoint != 0x5d /* ] */) {
                    if (codePoint == 0x5c /* \ */) {
                        parseEscape(inCharClass = true)
                        numTests++
                        continue
                    }

                    val start = codePoint
                    cursor++

                    if (!done && codePoint == 0x2d /* - */ && peek(1) != 0x5d /* ] */) {
                        cursor++
                        val end = codePoint

                        if (start > end)
                            error("Character class range is out-of-order")

                        +CharRangeOp(start, end)
                    } else {
                        +CharOp(start)
                    }

                    numTests++
                }

                if (!consumeIf(0x5d /* ] */))
                    error("Expected closing ']'")

                val classState = states.removeLast()
                +charClassOp(numTests, classState.size)
                state.merge(classState)
            }
            0x5c /* \ */ -> parseEscape(inCharClass = false)
            0x5e /* ^ */ -> {
                cursor++
                state.modifierMark = state.mark()
                +StartOp
            }
            0x24 /* $ */ -> {
                cursor++
                state.modifierMark = state.mark()
                +EndOp
            }
            0x2e /* . */ -> {
                cursor++
                state.modifierMark = state.mark()
                +AnyOp
            }
            else -> {
                if (codePoint in charsThatRequireEscape)
                    error("Unescaped \"${codePoint.toChar()}\"")

                state.modifierMark = state.mark()
                +CharOp(codePoint).also { cursor++ }
            }
        }
    }

    private fun parseEscape(inCharClass: Boolean) {
        cursor++
        if (done)
            error("RegExp cannot end with a backslash")

        state.modifierMark = state.mark()

        when (codePoint) {
            0x74 /* t */ -> +CharOp(0x9)
            0x6e /* n */ -> +CharOp(0xa)
            0x76 /* v */ -> +CharOp(0xb)
            0x72 /* r */ -> +CharOp(0xd)
            0x73 /* s */ -> +WhitespaceOp
            0x53 /* S */ -> {
                +NegateNextOp
                +WhitespaceOp
            }
            0x62 /* b */ -> if (inCharClass) {
                +CharOp(0x8 /* <backspace> */)
            } else +WordBoundaryOp
            0x42 /* B */ -> {
                +NegateNextOp
                +WordBoundaryOp
            }
            0x64 /* d */ -> +DigitOp
            0x44 /* D */ -> {
                +NegateNextOp
                +DigitOp
            }
            0x77 /* w */ -> +WordOp
            0x57 /* W */ -> {
                +NegateNextOp
                +WordOp
            }
            in 0x30..0x39 /* 1-9 */ -> {
                // Check for back reference, which would be in decimal. This could also be
                // an octal escape, but that will be determined after the regexp is fully
                // parsed (we don't know if this backref is valid yet)
                val result = parseNumber(1, 3, base = 10) ?: unreachable()
                result.consume()
                indexedBackrefs.add(state.mark() to result)
                +BackReferenceOp(result.value)
            }
            0x75 /* u */ -> {
                parseUnicodeEscapeSequence()
                return
            }
            0x78 /* x */ -> {
                cursor++
                +CharOp(parseNumber(2, 2, base = 16)?.value ?: 0x78)

                // Negate the cursor increment that comes after this loop
                cursor--
            }
            0x70, 0x50 /* p, P */ -> {
                if (codePoint == 0x50)
                    +NegateNextOp

                cursor++
                if (!consumeIf(0x7b /* { */))
                    error("Expected '{'")

                val text = parseName(endDelimiter = 0x7d /* } */)

                if (text !in unicodePropertyAliasList && text !in unicodeValueAliasesList)
                    error("Unknown unicode property or category \"$text\"")

                +UnicodeClassOp(text)

                // Negate the cursor increment that comes after this loop
                cursor--
            }
            0x6b /* k */ -> {
                cursor++
                if (consumeIf(0x3c /* < */)) {
                    val name = parseName(endDelimiter = 0x3e /* > */)
                    +BackReferenceOp(name)
                    backrefNames.add(name)
                } else {
                    +CharOp(0x6b)
                }

                // Negate the cursor increment that comes after this loop
                cursor--
            }
            else -> +CharOp(codePoint)
        }

        cursor++
    }

    private fun parseUnicodeEscapeSequence() {
        expect(codePoint == 0x75 /* u */)
        cursor++

        val start = cursor

        fun incomplete() {
            if (unicode)
                error("Incomplete unicode escape sequence")
            cursor = start
            +CharOp(0x75 /* u */)
        }

        if (done)
            return incomplete()

        if (consumeIf(0x7b /* { */)) {
            val numberInfo = parseNumber(1, Int.MAX_VALUE, base = 16) ?: return incomplete()
            if (numberInfo.value > 0x10ffff)
                error("Invalid unicode escape sequence; codepoint is greater than the max value of 0x10ffff")

            numberInfo.consume()

            if (done || codePoint != 0x7d /* } */)
                return incomplete()

            cursor++
            +CharOp(numberInfo.value)
        } else {
            val numberInfo = parseNumber(4, 4, base = 16) ?: return incomplete()
            if (done)
                return incomplete()

            numberInfo.consume()
            +CharOp(numberInfo.value)
        }
    }

    private fun parseSecondary() {
        when (codePoint) {
            0x3f /* ? */ -> {
                cursor++
                val lazy = consumeIf(0x3f /* ? */)

                /*
                 * Fork/ForkNow X+1
                 * <X opcodes>
                 */

                val modifierMark = state.modifierMark
                val numTargetOpcodes = state.size - modifierMark.offset

                val op = if (lazy) ::ForkNow else ::Fork
                modifierMark.insertBefore(op(numTargetOpcodes + 1))
            }
            0x2a /* * */ -> {
                cursor++
                val lazy = consumeIf(0x3f /* ? */)

                /*
                 * Fork/ForkNow X+2
                 * <X opcodes>
                 * ForkNow/Fork -X
                 */

                val modifierMark = state.modifierMark
                val numTargetOpcodes = state.size - modifierMark.offset

                val firstOp = if (lazy) ::ForkNow else ::Fork
                val secondOp = if (lazy) ::Fork else ::ForkNow

                modifierMark.insertBefore(firstOp(numTargetOpcodes + 2)) // Skip 'secondOp'
                +secondOp(-numTargetOpcodes)
            }
            0x2b /* + */ -> {
                cursor++
                val lazy = consumeIf(0x3f /* ? */)

                /*
                 * <X opcodes>
                 * Fork/ForkNow -X
                 */

                val numTargetOpcodes = state.size - state.modifierMark.offset
                val op = if (lazy) ::Fork else ::ForkNow
                +op(-numTargetOpcodes)
            }
            0x7c /* | */ -> {
                cursor++

                /*
                 * Fork X+2
                 * <X opcodes>
                 * Jump Y+1
                 * <Y opcodes>
                 */

                val alternationMark = state.alternationMark
                val numTargetOpcodes = state.size - alternationMark.offset

                alternationMark.insertBefore(Fork(numTargetOpcodes + 2)) // Skip the jump
                +Jump(numTargetOpcodes)

                state.alternationMark = state.mark()
            }
            0x7b /* { */ -> parseBracketedRepetition()
            else -> {}
        }
    }

    private fun parseBracketedRepetition() {
        /*
         * RangeCheck [A, B]
         * JumpIfBelowRange +3
         * JumpIfAboveRange X+2
         * Fork/ForkNow X+1
         * <X opcodes>
         * Jump -X-5
         */

        expect(codePoint == 0x7b /* { */)
        cursor++
        val start = cursor

        fun incomplete() {
            cursor = start
            +CharOp(0x7b)
        }

        val modifierMark = state.modifierMark
        val numTargetOpcodes = state.size - modifierMark.offset

        val firstBound = parseNumber(1, 8, base = 10)?.value ?: return incomplete()

        if (done)
            return incomplete()

        val secondBound = if (consumeIf(0x2c /* , */)) {
            parseNumber(1, 8, base = 10)?.value
        } else firstBound

        if (secondBound != null && secondBound > firstBound)
            error("Quantifier range is out of order")

        if (!consumeIf(0x7d /* } */))
            return incomplete()

        val lazy = consumeIf(0x3f /* ? */)

        modifierMark.insertBefore(RangeCheck(firstBound, secondBound))
        modifierMark.insertBefore(JumpIfBelowRange(3))
        modifierMark.insertBefore(JumpIfAboveRange(numTargetOpcodes + 2))

        val forkOp = if (lazy) ::ForkNow else ::Fork
        modifierMark.insertBefore(forkOp(numTargetOpcodes + 1))

        +Jump(-numTargetOpcodes - 5)
    }

    private fun codePointToInt(cp: Int, base: Int): Int? {
        if (cp in 0x30..0x39 /* 0-9 */)
            return (cp - 0x30).takeIf { it < base }

        if (cp in 0x41..0x5a /* A-Z */)
            return (cp - 0x41 + 10).takeIf { it < base }

        if (cp in 0x61..0x7a /* a-z */)
            return (cp - 0x61 + 10).takeIf { it < base }

        return null
    }

    private inner class NumberInfo(val codePoints: IntArray, val value: Int) {
        fun consume() {
            cursor += codePoints.size
        }
    }

    private fun parseNumber(min: Int, max: Int, base: Int): NumberInfo? {
        expect(min >= 0)
        expect(base <= 16)

        var value = 0
        var numChars = 0

        while (cursor + numChars <= codePoints.lastIndex && numChars < max) {
            val digitValue = codePointToInt(codePoints[cursor + numChars], base) ?: break
            value = (value shl 4) or digitValue
            numChars++
        }

        if (numChars !in min..max)
            return null

        return NumberInfo(codePoints.copyOfRange(cursor, cursor + numChars), value)
    }

    private fun parseName(endDelimiter: Int): String {
        val start = cursor
        val name = consumeUntil(endDelimiter) ?: error("Expected valid name")

        if (done || codePoint != endDelimiter)
            error("Expected '${endDelimiter.toChar()}")

        cursor++

        name.forEachIndexed { index, ch ->
            if (!isWordCodepoint(ch)) {
                cursor = start + index
                error("Invalid character '${ch.toChar()}' in name")
            }
        }

        return name.codePointsToString()
    }

    private fun consumeUntil(delimiter: Int): IntArray? {
        val start = cursor
        while (!done && codePoint != delimiter)
            cursor++

        return if (cursor != start) {
            codePoints.copyOfRange(start, cursor)
        } else null
    }

    private fun consumeIf(vararg codePoints: Int): Boolean {
        if (cursor + codePoints.size > this.codePoints.size)
            return false
        
        for ((i, cp) in codePoints.withIndex()) {
            if (this.codePoints[cursor + i] != cp)
                return false
        }

        cursor += codePoints.size
        return true
    }

    private fun peek(n: Int): Int? = codePoints.getOrNull(cursor + n)

    private fun error(message: String): Nothing = throw RegexSyntaxError(message, cursor)

    private operator fun Opcode.unaryPlus() = state.addOpcode(this)

    companion object {
        private val charsThatRequireEscape = setOf(
            '+', '*', '?', '(', ')', '[', '/'
        ).map(Char::code)
    }
}
