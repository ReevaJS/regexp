package com.reeva.regexp

data class RegExpState(
    var source: String,
    var cursor: Int = 0,
) {
    private val markedCursors = mutableListOf<Int>()

    val char: Char
        get() = source[cursor]

    val done: Boolean
        get() = !has(0)

    fun has(n: Int) = cursor + n <= source.lastIndex

    fun peek(n: Int) = source[cursor + n]

    fun advance() {
        cursor++
    }

    fun mark() {
        markedCursors.add(cursor)
    }

    fun discardMark() {
        markedCursors.removeLast()
    }

    fun loadMark() {
        cursor = markedCursors.removeLast()
    }
}
