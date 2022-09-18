package com.reevajs.regexp

class ASTParser(private val codePoints: IntArray, private val unicode: Boolean) {
    private var nextGroupIndex = 1
    private var cursor = 0
    private val namedGroups = mutableMapOf<Int, String>()
    private val groupNames = mutableSetOf<String>()
    private val unresolvedBackRefs = mutableListOf<Pair<BackReferenceNode, String>>()

    private val nodeBuffers = mutableListOf<MutableList<ASTNode>>()

    private val codePoint: Int
        get() = codePoints[cursor]

    private val done: Boolean
        get() = cursor > codePoints.lastIndex

    fun parse(): RootNode {
        pushNodeBuffer()

        while (!done)
            parseSingle()

        for ((ref, name) in unresolvedBackRefs) {
            if (name !in groupNames)
                error("Named reference to non-existent group \"$name\"")
            ref.index = namedGroups.entries.first { it.value == name }.key
        }

        return RootNode(namedGroups, popNodeBuffer()).also {
            expect(nodeBuffers.isEmpty())
        }
    }

    private fun parseSingle() {
        parseNode()
        tryParseSecondary()
    }

    private fun parseNode() {
        when (codePoint) {
            '('.code -> {
                cursor++

                pushNodeBuffer()

                val groupPrefix = when {
                    consumeIf('?'.code, ':'.code) -> "?:"
                    consumeIf('?'.code, '='.code) -> "?="
                    consumeIf('?'.code, '!'.code) -> "?!"
                    consumeIf('?'.code, '<'.code, '='.code) -> "?<="
                    consumeIf('?'.code, '<'.code, '!'.code) -> "?<!"
                    consumeIf('?'.code, '<'.code) -> {
                        val name = parseName(endDelimiter = '>'.code)
                        if (name in groupNames)
                            error("Duplicate capturing group name \"$name\"")

                        namedGroups[nextGroupIndex] = name
                        "?<$name>"
                    }
                    else -> null
                }

                if (groupPrefix != null)
                    cursor += groupPrefix.length

                while (!done && codePoint != ')'.code)
                    parseSingle()

                if (!consumeIf(')'.code))
                    error("Expected ')'")

                val nodes = popNodeBuffer()

                when (groupPrefix) {
                    "?:" -> +GroupNode(null, nodes)
                    "?=" -> +PositiveLookaheadNode(nodes + MatchNode)
                    "?!" -> +NegativeLookaheadNode(nodes + MatchNode)
                    "?<=" -> +PositiveLookbehindNode(nodes + MatchNode)
                    "?<!" -> +NegativeLookbehindNode(nodes + MatchNode)
                    else -> +GroupNode(nextGroupIndex++, nodes)
                }
            }
            '['.code -> {
                cursor++

                val node = if (consumeIf('^'.code)) {
                    cursor++
                    ::InvertedCharacterClassNode
                } else ::CharacterClassNode

                pushNodeBuffer()

                while (!done && codePoint != ']'.code) {
                    if (codePoint == '\\'.code) {
                        parseEscape(inCharClass = true)
                        continue
                    }

                    val start = codePoint
                    cursor++

                    if (!done && codePoint != '-'.code && peek(1) != ']'.code) {
                        cursor++
                        val end = codePoint

                        if (start > end)
                            error("Character class range is out of order")

                        +CodePointRangeNode(start, end)
                    } else {
                        +CodePointNode(start)
                    }
                }

                if (!consumeIf(']'.code))
                    error("Expected closing ']'")

                +node(popNodeBuffer())
            }
            '\\'.code -> parseEscape(inCharClass = false)
            '^'.code -> {
                cursor++
                +StartNode
            }
            '$'.code -> {
                cursor++
                +EndNode
            }
            '.'.code -> {
                cursor++
                +AnyNode
            }
            else -> {
                if (codePoint in charsThatRequireEscape)
                    error("Unescaped \"${codePoint.toChar()}\"")

                +CodePointNode(codePoint)
                cursor++
            }
        }
    }

    private fun tryParseSecondary() {
        if (done || nodeBuffers.isEmpty())
            return

        when (codePoint) {
            '?'.code -> {
                cursor++
                +ZeroOrOneNode(popNode(), consumeIf('?'.code))
            }
            '*'.code -> {
                cursor++
                +ZeroOrMoreNode(popNode(), consumeIf('?'.code))
            }
            '+'.code -> {
                cursor++
                +OneOrMoreNode(popNode(), consumeIf('?'.code))
            }
            '|'.code -> {
                cursor++
                val lhs = GroupNode(null, popNodeBuffer())
                pushNodeBuffer()

                while (!done && codePoint != ')'.code)
                    parseSingle()

                +AlternationNode(lhs, GroupNode(null, popNodeBuffer()))
            }
            '{'.code -> {
                val start = codePoint

                fun incomplete() {
                    cursor = start
                    +CodePointNode('{'.code)
                }

                val firstBound = parseNumber(1, 8, base = 10)?.let {
                    it.consume()
                    it.value
                } ?: return incomplete()

                if (done)
                    return incomplete()

                val secondBound = if (consumeIf(','.code)) {
                    parseNumber(1, 8, base = 10)?.let {
                        it.consume()
                        it.value
                    }
                } else firstBound

                if (!consumeIf('}'.code))
                    return incomplete()

                if (secondBound != null && secondBound > firstBound)
                    error("Quantifier range is out of order")

                +RepetitionNode(popNode(), firstBound, secondBound, consumeIf('?'.code))
            }
        }
    }

    private fun parseEscape(inCharClass: Boolean) {
        cursor++
        if (done)
            error("RegExp cannot end with a backslash")

        when (codePoint) {
            't'.code -> +CodePointNode('\t'.code)
            'n'.code -> +CodePointNode('\n'.code)
            'v'.code -> +CodePointNode(0xb)
            'r'.code -> +CodePointNode('\r'.code)
            's'.code -> +WhitespaceNode
            'S'.code -> {
                +NegateNextNode
                +WhitespaceNode
            }
            'b'.code -> if (inCharClass) {
                +CodePointNode(0x8)
            } else +WordBoundaryNode
            'B'.code -> {
                +NegateNextNode
                +WordBoundaryNode
            }
            'd'.code -> +DigitNode
            'D'.code -> {
                +NegateNextNode
                +DigitNode
            }
            'w'.code -> +WordNode
            'W'.code -> {
                +NegateNextNode
                +WordNode
            }
            in '1'.code..'9'.code -> {
                val result = parseNumber(1, 3, base = 10) ?: unreachable()
                result.consume()

                +BackReferenceNode(result.value)
            }
            'u'.code -> {
                parseUnicodeEscapeSequence()
                return
            }
            'x'.code -> {
                cursor++
                +CodePointNode(parseNumber(2, 2, base = 16)?.value ?: 0x78)
                return
            }
            'p'.code, 'P'.code -> {
                if (codePoint == 'P'.code)
                    +NegateNextNode

                cursor++
                if (!consumeIf('{'.code))
                    error("Expected '{'")

                val text = parseName(endDelimiter = 0x7d /* } */)

                if (text !in unicodePropertyAliasList && text !in unicodeValueAliasesList)
                    error("Unknown unicode property or category \"$text\"")

                +UnicodeClassNode(text)
                return
            }
            'k'.code -> {
                cursor++
                if (consumeIf(0x3c /* < */)) {
                    val name = parseName(endDelimiter = 0x3e /* > */)
                    // No need to bother checking if this exists here, just assume it doesn't and
                    // resolve all named back references at the end
                    val node = BackReferenceNode(Int.MIN_VALUE)
                    +node
                    unresolvedBackRefs.add(node to name)
                } else {
                    +CodePointNode(0x6b)
                }

                return
            }
            else -> +CodePointNode(codePoint)
        }

        cursor++
    }

    private fun parseUnicodeEscapeSequence() {
        expect(codePoint == 'u'.code)
        cursor++

        val start = cursor

        fun incomplete() {
            if (unicode)
                error("Incomplete unicode escape sequence")
            cursor = start
            +CodePointNode('u'.code)
        }

        if (done)
            return incomplete()

        if (consumeIf('{'.code)) {
            val numberInfo = parseNumber(1, Int.MAX_VALUE, base = 16) ?: return incomplete()
            if (numberInfo.value > 0x10ffff)
                error("Invalid unicode escape sequence; codepoint is greater than the max value of 0x10ffff")

            numberInfo.consume()

            if (done || codePoint != '}'.code)
                return incomplete()

            cursor++
            +CodePointNode(numberInfo.value)
        } else {
            val numberInfo = parseNumber(4, 4, base = 16) ?: return incomplete()
            if (done)
                return incomplete()

            numberInfo.consume()
            +CodePointNode(numberInfo.value)
        }
    }

    private inner class NumberInfo(val codePoints: IntArray, val value: Int) {
        fun consume() {
            cursor += codePoints.size
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

    private operator fun ASTNode.unaryPlus() {
        nodeBuffers.last().add(this)
    }

    private fun popNode() = nodeBuffers.last().removeLast()

    private fun pushNodeBuffer() {
        nodeBuffers.add(mutableListOf())
    }

    private fun popNodeBuffer(): List<ASTNode> = nodeBuffers.removeLast()

    companion object {
        private val charsThatRequireEscape = setOf(
            '+', '*', '?', '(', ')', '[', '/'
        ).map(Char::code)
    }
}
