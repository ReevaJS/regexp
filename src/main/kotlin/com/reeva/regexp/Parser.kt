package com.reeva.regexp

class Parser(private val source: String) {
    private var cursor = 0

    private val char: Char
        get() = source[cursor]

    private val done: Boolean
        get() = cursor > source.lastIndex

    fun parse(): RootAST {
        val topLevelASTs = mutableListOf<AST>()

        while (!done)
            topLevelASTs.add(parseSingle())

        return Optimizer.optimize(RootAST(GroupAST(topLevelASTs))) as RootAST
    }

    private fun parseSingle(): AST {
        var target = parsePrimary()
    
        while (!done)
            target = parseSecondary(target) ?: return target

        return target
    }

    private fun parsePrimary(): AST {
        return when (char) {
            '(' -> {
                cursor++
                val asts = mutableListOf<AST>()

                while (!done && char != ')')
                    asts.add(parseSingle())

                if (char != ')')
                    throw IllegalStateException("Expected ')'")

                cursor++

                GroupAST(asts)
            }
            '[' -> {
                cursor++

                val inverted = consumeIf('^')
                val ranges = mutableListOf<CharacterRange>()

                while (!done && char != ']') {
                    if (char == '\\')
                        TODO()

                    val start = char
                    cursor++

                    val end = if (!done && char == '-' && peek(1) != ']') {
                        cursor++
                        char.also { cursor++ }
                    } else start

                    if (start > end)
                        throw IllegalStateException("Range is out-of-order")

                    ranges.add(CharacterRange(start, end))
                }

                if (char != ']')
                    throw IllegalStateException("Expected ']'")

                cursor++

                CharacterClassAST(ranges, inverted)
            }
            '^' -> {
                cursor++
                StartAST
            }
            '$' -> {
                cursor++
                EndAST
            }
            '\\' -> parseEscape(inCharClass = false)
            '.' -> {
                cursor++
                AnyAST
            }
            else -> {
                if (char in charsThatRequireEscape)
                    throw IllegalStateException("Character requires escape: '$char'")

                CharAST(char).also { cursor++ }
            }
        }
    }

    private fun parseSecondary(target: AST): AST? {
        return when (char) {
            '*' -> {
                cursor++
                RepetitionRangeAST(target, 0, null, consumeIf('?'))
            }
            '+' -> {
                cursor++
                RepetitionRangeAST(target, 1, null, consumeIf('?'))
            }
            '{' -> {
                cursor++

                val lowerBound = buildString {
                    while (!done && char.isDigit()) {
                        append(char)
                        cursor++
                    }
                }.toInt()

                when (char) {
                    '}' -> {
                        cursor++
                        consumeIf('?') // This doesn't do anything, but it is valid
                        RepetitionAST(target, lowerBound)
                    }
                    ',' -> {
                        cursor++
                        if (char == '}') {
                            cursor++
                            RepetitionRangeAST(target, lowerBound, null, consumeIf('?'))
                        } else {
                            val upperBound = buildString {
                                while (!done && char.isDigit()) {
                                    append(char)
                                    cursor++
                                }
                            }.toInt()

                            if (char != '}')
                                throw IllegalStateException("Expected '}'")

                            cursor++

                            RepetitionRangeAST(target, lowerBound, upperBound, consumeIf('?'))
                        }
                    }
                    else -> throw IllegalStateException("Unexpected char in range: '$char'")
                }
            }
            '?' -> {
                cursor++
                RepetitionRangeAST(target, 0, 1, consumeIf('?'))
            }
            '|' -> {
                cursor++
                AlternationAST(listOf(target, parseSingle()))
            }
            else -> null
        }
    }

    private fun parseEscape(inCharClass: Boolean): AST {
        if (char != '\\')
            throw IllegalStateException()

        cursor++

        if (done)
            throw IllegalStateException("RegExp cannot end with '\\'")

        return when (char) {
            'n' -> CharAST('\n')
            't' -> CharAST('\t')
            'v' -> CharAST(11.toChar())
            '.' -> CharAST('.')
            'b' -> if (inCharClass) {
                CharAST('\b')
            } else WordBoundaryAST(inverted = false)
            'B' -> WordBoundaryAST(inverted = true)
            's' -> WhitespaceAST(inverted = false)
            'S' -> WhitespaceAST(inverted = true)
            'd' -> DigitAST(inverted = false)
            'D' -> DigitAST(inverted = true)
            'w' -> WordAST(inverted = false)
            'W' -> WordAST(inverted = true)
            else -> CharAST(char)
        }.also { cursor++ }
    }

    private fun consumeIf(char: Char): Boolean {
        return if (!done && this.char == char) {
            cursor++
            true
        } else false
    }

    private fun peek(n: Int): Char? {
        return source.getOrNull(cursor + n)
    }

    companion object {
        private val charsThatRequireEscape = setOf(
            '*', '?', '(', ')', '[', '/'
        )
    }
}