package com.reeva.regexp

data class MatchGroup(
    val codePoints: IntArray,
    val range: IntRange,
) {
    val value = codePoints.codePointsToString()

    fun copy() = MatchGroup(codePoints.copyOf(), range)

    override fun toString() = "MatchGroup(\"$value\", range=$range)"
}

data class MatchResult(
    val indexedGroups: List<MatchGroup>,
    val namedGroups: Map<String, MatchGroup>,
) {
    val groupValues: List<String>
        get() = indexedGroups.map { it.value }

    val range: IntRange
        get() = indexedGroups.first().range.first..indexedGroups.last().range.last

    override fun toString() = buildString {
        append("MatchResult(")
        if (indexedGroups.isEmpty() && namedGroups.isEmpty()) {
            append("<empty>)")
            return@buildString
        }

        for ((index, group) in indexedGroups.withIndex()) {
            appendGroup(index, group)

            if (index != indexedGroups.lastIndex)
                append(", ")
        }

        if (indexedGroups.isNotEmpty() && namedGroups.isNotEmpty())
            append(", ")

        for ((index, group) in namedGroups.entries.withIndex()) {
            appendGroup(group.key, group.value)

            if (index != namedGroups.size - 1)
                append(", ")
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
