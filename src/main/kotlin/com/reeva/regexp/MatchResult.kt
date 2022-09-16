package com.reeva.regexp

class MatchGroup(
    val codePoints: IntArray,
    val range: IntRange,
) {
    val value = codePoints.codePointsToString()

    fun copy() = MatchGroup(codePoints.copyOf(), range)

    override fun toString() = "MatchGroup(\"$value\", range=$range)"
}

data class MatchResult(
    val groups: List<MatchGroup>,
    val namedGroups: Map<String, MatchGroup>,
) {
    val groupValues: List<String>
        get() = groups.map { it.value }
}
