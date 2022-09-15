package com.reeva.regexp

class Parser(private val codepoints: IntArray, private val unicode: Boolean) {
    private val builder = OpcodeBuilder()
    private var nextGroupIndex = 0
    private var cursor = 0

    private val states = mutableListOf<State>()
    private val state: State
        get() = states.last()

    private val codepoint: Int
        get() = codepoints[cursor]

    private val done: Boolean
        get() = cursor > codepoints.lastIndex

    constructor(source: String, unicode: Boolean) : this(source.codePoints().toArray(), unicode)

    fun parse(): List<Opcode> {
        states.add(State())

        while (!done)
            parseSingle()

        expect(states.size == 1)

        return builder.build()
    }

    private fun parseSingle() {
        parsePrimary()
        parseSecondary()
    }

    private fun parsePrimary() {
        when (codepoint) {
            0x28 /* ( */ -> {
                cursor++
                states.add(State())

                +StartGroupOp(nextGroupIndex++)


                while (!done && codepoint != 0x29 /* ) */)
                    parseSingle()

                if (codepoint != 0x29 /* ) */)
                    error("Expected ')'")

                states.removeLast()

                cursor++
                +EndGroupOp
            }
            0x5b /* [ */ -> {
                cursor++
                TODO()
            }
            0x5c /* \ */ -> parseEscape(inCharClass = false)
            else -> TODO()
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

                    v
                } else parseHexValue(4, 4)

                if (codepoint >= 0x10ffff)
                    error("Codepoint ${codepoint.toString(radix = 16)} is too large")

                +CharOp(codepoint)
            }
            0x78 /* x */ -> {
                cursor++
                +CharOp(parseHexValue(2, 2))
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

                /*
                 * Fork X+1
                 * <X opcodes>
                 */

                val modifierMark = state.modifierMark
                val numTargetOpcodes = builder.size - modifierMark.offset

                modifierMark.insertBefore(Fork(numTargetOpcodes + 1))

                if (codepoint == 0x3f /* ? */)
                    TODO()

                state.modifierMark = builder.mark()
            }
            0x2a /* * */ -> {
                cursor++

                /*
                 * Fork X+1
                 * <X opcodes>
                 * Fork -X
                 */

                val modifierMark = state.modifierMark
                val numTargetOpcodes = builder.size - modifierMark.offset

                modifierMark.insertBefore(Fork(numTargetOpcodes + 1))
                +Fork(-numTargetOpcodes)

                if (codepoint == 0x3f /* ? */)
                    TODO()

                state.modifierMark = builder.mark()
            }
            0x2b /* + */ -> {
                cursor++

                /*
                 * <X opcodes>
                 * Fork -X
                 */

                val numTargetOpcodes = builder.size - state.modifierMark.offset
                +Fork(-numTargetOpcodes)

                if (codepoint == 0x3f /* ? */)
                    TODO()

                state.modifierMark = builder.mark()
            }
            0x7c /* | */ -> {
                cursor++

                /*

                ab|cd


                Char a
                Char b



                Fork +4
                Char a
                Char b
                Jump +3
                Char c
                Char d
                <end>

                 */
            }
        }
    }

    private fun parseHexValue(min: Int, max: Int): Int {
        expect(min >= 0 && max <= 8)

        var value = 0
        var numChars = 0

        while (!done && numChars < max) {
            if (codepoint in 0x30..0x39 /* 0-9 */ ) {
                value = (value shl 4) or (codepoint - 0x30)
                numChars++
                cursor++
            } else break
        }

        if (numChars !in min..max)
            error("Expected $min-$max hexadecimal characters")

        return value
    }

    private fun error(message: String): Nothing = throw RegexSyntaxError(message, cursor)

    private operator fun Opcode.unaryPlus() = builder.addOpcode(this)

    private inner class State(
        var alternationMark: OpcodeBuilder.Mark = builder.mark(),
        var modifierMark: OpcodeBuilder.Mark = builder.mark(),
    )
}
