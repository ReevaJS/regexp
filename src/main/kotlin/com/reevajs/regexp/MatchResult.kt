package com.reevajs.regexp

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
    val groups: SortedMap<Int, MatchGroup>,
    val groupNames: Map<Int, String>,
) {
    val range: IntRange by lazy {
        var start: Int? = null
        var end: Int? = null

        groups.values.forEach { group ->
            start = min(group.range.first, start ?: Int.MAX_VALUE)
            end = max(group.range.last, start ?: Int.MIN_VALUE)
        }

        start!!..end!!
    }

    val namedGroups: List<Pair<String, MatchGroup>>
        get() = groupNames.map { (index, name) -> name to groups[index]!! }

    override fun toString() = buildString {
        append("MatchResult(")
        if (groups.isEmpty()) {
            append("<empty>)")
            return@buildString
        }

        var first = true
        for ((index, group) in groups) {
            if (!first)
                append(", ")
            first = false

            append(index)
            if (index in groupNames)
                append(" (${groupNames[index]})")
            append(": \"${group.value}\" [${group.range.first}..${group.range.last}]")
        }

        append(")")
    }
}
