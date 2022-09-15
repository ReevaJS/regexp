package com.reeva.regexp

class Parser(private val codepoints: IntArray, private val unicode: Boolean) {
    private var builder = OpcodeBuilder()
    private var nextGroupIndex = 0
    private var cursor = 0
    private val groupNames = mutableSetOf<String>()

    private val states = mutableListOf<State>()
    private val state: State
        get() = states.last()

    private val codepoint: Int
        get() = codepoints[cursor]

    private val done: Boolean
        get() = cursor > codepoints.lastIndex

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
        when (codepoint) {
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
                        val nameBuilder = StringBuilder()
    
                        while (!done && codepoint != 0x3e /* > */)  {
                            nameBuilder.appendCodePoint(codepoint)
                            cursor++
                        }
    
                        if (codepoint != 0x3e /* > */)
                            error("Expected '>'")
    
                        cursor++
    
                        val name = nameBuilder.toString()
                        if (!groupNames.add(name))
                            error("Duplicate capturing group name \"$name\"")
    
                        +StartNamedGroupOp(name)
                    }
                    else -> +StartGroupOp(nextGroupIndex++)
                }

                while (!done && codepoint != 0x29 /* ) */)
                    parseSingle()

                if (codepoint != 0x29 /* ) */) 
                    error("Expected ')'")

                states.removeLast()
                cursor++

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

                val ops = mutableListOf<Opcode>()

                if (codepoint == 0x5e /* ^ */) {
                    cursor++
                    +NegateNextOp
                }

                while (!done && codepoint != 0x5d /* ] */) {
                    if (codepoint == 0x5c /* \ */)
                        TODO()

                    val start = codepoint
                    cursor++

                    if (!done && codepoint == 0x2d /* - */ && peek(1) != 0x5d /* ] */) {
                        cursor++
                        val end = codepoint

                        if (start > end)
                            error("Character class range is out-of-order")

                        ops.add(CharRangeOp(start, end))
                    } else {
                        ops.add(CharOp(start))
                    }
                }

                if (codepoint != 0x5d /* ] */)
                    error("Expected closing ']'")

                cursor++

                +CharClassOp(ops.size)
                ops.forEach { +it }
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
                if (codepoint in charsThatRequireEscape)
                    error("Unescaped \"${codepoint.toChar()}\"")

                state.modifierMark = builder.mark()
                +CharOp(codepoint).also { cursor++ }
            }
        }
    }

    private fun parseEscape(inCharClass: Boolean) {
        cursor++
        if (done)
            error("RegExp cannot end with a backslash")

        state.modifierMark = builder.mark()

        when (codepoint) {
            0x74 /* t */ -> +CharOp(0x9)
            0x6e /* n */ -> +CharOp(0xa)
            0x76 /* v */ -> +CharOp(0xb)
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
            in 0x30..0x39 /* 1-9 */ -> +BackReferenceOp(codepoint - 0x30)
            0x75 /* u */ -> {
                cursor++

                val codepoint = if (unicode && codepoint == 0x7b /* { */) {
                    cursor++
                    val v = parseHexValue(1, 6)
                    if (codepoint != 0x7d /* } */)
                        error("Expected '}' to close unicode escape sequence")

                    cursor++
                    v
                } else parseHexValue(4, 4)

                if (codepoint >= 0x10ffff)
                    error("Codepoint ${codepoint.toString(radix = 16)} is too large")

                +CharOp(codepoint)

                // Negate the cursor increment that comes after this loop
                cursor--
            }
            0x78 /* x */ -> {
                cursor++
                +CharOp(parseHexValue(2, 2))

                // Negate the cursor increment that comes after this loop
                cursor--
            }
            0x70, 0x50 /* p, P */ -> TODO()
            0x6b /* k */ -> TODO()
            else -> +CharOp(codepoint)
        }

        cursor++
    }

    private fun parseSecondary() {
        when (codepoint) {
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
                 * Fork/ForkNow X+1
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
                 *
                 * Fork X+2
                 * <X opcodes>
                 * Jump Y+1
                 * <Y opcodes>
                 *
                 */

                val alternationMark = state.alternationMark
                val numTargetOpcodes = builder.size - alternationMark.offset

                alternationMark.insertBefore(Fork(numTargetOpcodes + 2)) // Skip the jump
                +Jump(numTargetOpcodes + 1)
                state.alternationMark = builder.mark()
            }
            else -> return
        }
    }

    private fun parseHexValue(min: Int, max: Int): Int {
        expect(min >= 0 && max <= 8)

        var value = 0
        var numChars = 0

        while (!done && numChars < max) {
            if (codepoint in 0x30..0x39 /* 0-9 */) {
                value = (value shl 4) or (codepoint - 0x30)
                numChars++
                cursor++
            } else break
        }

        if (numChars !in min..max)
            error("Expected $min-$max hexadecimal characters")

        return value
    }

    private fun consumeIf(vararg codepoints: Int): Boolean {
        if (cursor + codepoints.size >= this.codepoints.lastIndex)
            return false

        for ((i, cp) in codepoints.withIndex()) {
            if (this.codepoints[cursor + i] != cp)
                return false
        }

        cursor += codepoints.size
        return true
    }

    private fun peek(n: Int): Int? = codepoints.getOrNull(cursor + n)

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
