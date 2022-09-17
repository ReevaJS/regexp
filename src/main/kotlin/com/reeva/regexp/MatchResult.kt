package com.reeva.regexp

import java.util.*
import kotlin.math.max
import kotlin.math.min

data class MatchGroup(
    val codePoints: IntArray,
    val range: IntRange,
) {
    val value = codePoints.codePointsToString()

    fun copy() = MatchGroup(codePoints.copyOf(), range)

    override fun toString() = "MatchGroup(\"$value\", range=$range)"

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true

        return other is MatchGroup && codePoints.contentEquals(other.codePoints) && range == other.range
    }

    override fun hashCode(): Int {
        var result = codePoints.contentHashCode()
        result = 31 * result + range.hashCode()
        return result
    }
}

data class MatchResult(
    val indexedGroups: SortedMap<Int, MatchGroup>,
    val namedGroups: Map<String, MatchGroup>,
) {
    val range: IntRange by lazy {
        var start: Int? = null
        var end: Int? = null

        indexedGroups.values.forEach { group ->
            start = min(group.range.first, start ?: Int.MAX_VALUE)
            end = max(group.range.last, start ?: Int.MIN_VALUE)
        }

        start!!..end!!
    }

    override fun toString() = buildString {
        append("MatchResult(")
        if (indexedGroups.isEmpty() && namedGroups.isEmpty()) {
            append("<empty>)")
            return@buildString
        }

        var first = true
        for ((index, group) in indexedGroups) {
            if (!first)
                append(", ")
            first = false

            appendGroup(index, group)
        }

        if (indexedGroups.isNotEmpty() && namedGroups.isNotEmpty())
            append(", ")

        first = true
        for (group in namedGroups) {
            if (!first)
                append(", ")
            first = false

            appendGroup(group.key, group.value)
        }

        append(")")
    }

    private fun StringBuilder.appendGroup(key: Any, group: MatchGroup) {
        append(key)
        append(": \"")
        append(group.value)
        append("\" [")
        append(group.range.first)
        append("..")
        append(group.range.last)
        append("]")
    }
}
