package com.reeva.regexp

class RegExp(
    private val regexCodePoints: IntArray, 
    private val flags: Set<Flag>,
) {
    constructor(regex: String, vararg flags: Flag) : this(regex.codePoints().toArray(), flags.toSet()) 

    fun test(text: String) = match(text) != null

    fun match(text: String): MatchResult? {
        // TODO: Use flags

        val opcodes = Parser(regexCodePoints, unicode = Flag.Unicode in flags).parse()
        return Matcher(text.codePoints().toArray(), opcodes).match()
    }

    enum class Flag {
        Global,
        MultiLine,
        Insensitive,
        Sticky,
        Unicode,
        SingleLine,
        Indices,
    }
}
