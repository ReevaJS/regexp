package com.reeva.regexp

class Parser(private val codePoints: IntArray, private val unicode: Boolean) {
    private var builder = OpcodeBuilder()
    private var nextGroupIndex = 0
    private var cursor = 0
    private val groupNames = mutableSetOf<String>()

    private val states = mutableListOf<State>()
    private val state: State
        get() = states.last()

    private val codePoint: Int
        get() = codePoints[cursor]

    private val done: Boolean
        get() = cursor > codePoints.lastIndex

    constructor(source: String, unicode: Boolean) : this(source.codePoints().toArray(), unicode)

    fun parse(): Array<Opcode> {
        states.add(State())

        // The entire match is implicitly group 0
        +StartGroupOp(nextGroupIndex++)

        while (!done)
            parseSingle()

        expect(states.size == 1)

        +EndGroupOp
        +MatchOp

        return builder.build()
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

                state.modifierMark = builder.mark()
                states.add(State())

                // TODO: This is very ugly
                val previousBuilder = builder
                builder = OpcodeBuilder()
                var lookOp: ((Array<Opcode>) -> LookOp)? = null

                when {
                    consumeIf(0x3f, 0x3a /* ?: */) -> +StartGroupOp(null)
                    consumeIf(0x3f, 0x3d /* ?= */) -> lookOp = ::PositiveLookaheadOp
                    consumeIf(0x3f, 0x21 /* ?! */) -> lookOp = ::NegativeLookaheadOp
                    consumeIf(0x3f, 0x3c, 0x3d /* ?<= */) -> lookOp = ::PositiveLookbehindOp
                    consumeIf(0x3f, 0x3c, 0x21 /* ?<! */) -> lookOp = ::NegativeLookbehindOp
                    consumeIf(0x3f, 0x3c /* ?< */) -> {
                        val name = parseName(endDelimiter = 0x3e /* > */)
                        if (!groupNames.add(name))
                            error("Duplicate capturing group name \"$name\"")

                        +StartNamedGroupOp(name)
                    }
                    else -> +StartGroupOp(nextGroupIndex++)
                }

                while (!done && codePoint != 0x29 /* ) */)
                    parseSingle()

                if (!consumeIf(0x29 /* ) */))
                    error("Expected ')'")

                states.removeLast()

                if (lookOp != null)
                    +MatchOp

                val ops = builder.build()
                builder = previousBuilder

                if (lookOp != null) {
                    +lookOp(ops)
                } else {
                    builder.addOpcodes(ops.toList())
                    +EndGroupOp
                }
            }
            0x5b /* [ */ -> {
                cursor++
                state.modifierMark = builder.mark()

                val charClassOp = if (codePoint == 0x5e /* ^ */) {
                    cursor++
                    ::InvertedCharClassOp
                } else ::CharClassOp

                val previousBuilder = builder
                builder = OpcodeBuilder()

                while (!done && codePoint != 0x5d /* ] */) {
                    if (codePoint == 0x5c /* \ */) {
                        parseEscape(inCharClass = true)
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
                }

                if (!consumeIf(0x5d /* ] */))
                    error("Expected closing ']'")

                val ops = builder.build()
                builder = previousBuilder

                +charClassOp(ops.size)
                builder.addOpcodes(ops.toList())
            }
            0x5c /* \ */ -> parseEscape(inCharClass = false)
            0x5e /* ^ */ -> {
                cursor++
                state.modifierMark = builder.mark()
                +StartOp
            }
            0x24 /* $ */ -> {
                cursor++
                state.modifierMark = builder.mark()
                +EndOp
            }
            0x2e /* . */ -> {
                cursor++
                state.modifierMark = builder.mark()
                +AnyOp
            }
            else -> {
                if (codePoint in charsThatRequireEscape)
                    error("Unescaped \"${codePoint.toChar()}\"")

                state.modifierMark = builder.mark()
                +CharOp(codePoint).also { cursor++ }
            }
        }
    }

    private fun parseEscape(inCharClass: Boolean) {
        cursor++
        if (done)
            error("RegExp cannot end with a backslash")

        state.modifierMark = builder.mark()

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
                // Check for back reference, which would be in hex
                var result = parseNumericValueAndLength(1, 3, base = 16)
                if (result != null && result.first < nextGroupIndex) {
                    cursor += result.second
                    +BackReferenceOp(result.first)
                } else {
                    // Check for octal escape
                    result = parseNumericValueAndLength(1, 3, base = 8)
                    if (result == null) {
                        cursor++
                        +CharOp(codePoint)
                    } else {
                        cursor += result.second
                        +CharOp(result.first)
                    }
                }
            }
            0x75 /* u */ -> {
                cursor++

                // TODO: This isn't quite correct for invalid hex. For example, "\u{zzzz}"
                // should parse as the character "u" followed by the characters "{zzzz}"
                val codePoint = if (unicode && codePoint == 0x7b /* { */) {
                    cursor++
                    parseNumericValue(1, 6, base = 16).also {
                        if (!consumeIf(0x7d /* } */))
                            error("Expected '}' to close unicode escape sequence")
                    }
                } else parseNumericValue(4, 4, base = 16)

                if (codePoint == null)
                    error("Expected hexadecimal number")

                if (codePoint >= 0x10ffff)
                    error("Codepoint ${codePoint.toString(radix = 16)} is too large")

                +CharOp(codePoint)

                // Negate the cursor increment that comes after this loop
                cursor--
            }
            0x78 /* x */ -> {
                cursor++
                +CharOp(parseNumericValue(2, 2, base = 16) ?: 0x78)

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
                    +BackReferenceOp(parseName(endDelimiter = 0x3e /* > */))
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
                val numTargetOpcodes = builder.size - modifierMark.offset

                val op = if (lazy) ::ForkNow else ::Fork
                modifierMark.insertBefore(op(numTargetOpcodes + 1))

                state.modifierMark = builder.mark()
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
                val numTargetOpcodes = builder.size - modifierMark.offset

                val firstOp = if (lazy) ::ForkNow else ::Fork
                val secondOp = if (lazy) ::Fork else ::ForkNow

                modifierMark.insertBefore(firstOp(numTargetOpcodes + 2)) // Skip 'secondOp'
                +secondOp(-numTargetOpcodes)

                state.modifierMark = builder.mark()
            }
            0x2b /* + */ -> {
                cursor++
                val lazy = consumeIf(0x3f /* ? */)

                /*
                 * <X opcodes>
                 * Fork/ForkNow -X
                 */

                val numTargetOpcodes = builder.size - state.modifierMark.offset
                val op = if (lazy) ::Fork else ::ForkNow
                +op(-numTargetOpcodes)

                state.modifierMark = builder.mark()
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
                val numTargetOpcodes = builder.size - alternationMark.offset

                alternationMark.insertBefore(Fork(numTargetOpcodes + 2)) // Skip the jump
                +Jump(numTargetOpcodes + 1)
                state.alternationMark = builder.mark()
            }
            0x7b /* { */ -> {
                cursor++

                /*
                 * RangeCheck [A, B]
                 * JumpIfBelowRange +3
                 * JumpIfAboveRange X+2
                 * Fork/ForkNow X+1
                 * <X opcodes>
                 * Jump -X-5
                 */

                val modifierMark = state.modifierMark
                val numTargetOpcodes = builder.size - modifierMark.offset

                val firstBound = parseNumericValue(1, 8, base = 10) ?: error("Expected number")

                if (done)
                    error("Expected ',' or '}'")

                val secondBound = if (consumeIf(0x2c /* , */)) {
                    parseNumericValue(1, 8, base = 10)
                } else firstBound

                if (!consumeIf(0x7d /* } */))
                    error("Expected '}'")

                val lazy = consumeIf(0x3f /* ? */)

                modifierMark.insertBefore(RangeCheck(firstBound, secondBound))
                modifierMark.insertBefore(JumpIfBelowRange(3))
                modifierMark.insertBefore(JumpIfAboveRange(numTargetOpcodes + 2))

                val forkOp = if (lazy) ::ForkNow else ::Fork
                modifierMark.insertBefore(forkOp(numTargetOpcodes + 1))

                +Jump(-numTargetOpcodes - 5)
            }
            else -> return
        }
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

    private fun parseNumericValueAndLength(min: Int, max: Int, base: Int): Pair<Int, Int>? {
        expect(min >= 0 && max <= 8)
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

        return value to numChars
    }

    private fun parseNumericValue(min: Int, max: Int, base: Int): Int? {
        val result = parseNumericValueAndLength(min, max, base) ?: return null
        cursor += result.second
        return result.first
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

    private operator fun Opcode.unaryPlus() = builder.addOpcode(this)

    private inner class State(
        var alternationMark: OpcodeBuilder.Mark = builder.mark(),
        var modifierMark: OpcodeBuilder.Mark = builder.mark(),
    )

    companion object {
        private val charsThatRequireEscape = setOf(
            '*', '?', '(', ')', '[', '/'
        ).map(Char::code)
    }
}
