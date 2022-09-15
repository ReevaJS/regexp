package com.reeva.regexp

class RegExp(
    private val regexCodePoints: IntArray, 
    private val flags: Set<Flag>,
) {
    constructor(regex: String, vararg flags: Flag) : this(regex.codePoints().toArray(), flags.toSet()) 

    fun test(text: String) = match(text) != null

    fun match(text: String) = matcher(text).match()

    fun matcher(text: String): Matcher {
        // TODO: Use flags

        val opcodes = Parser(regexCodePoints, unicode = Flag.Unicode in flags).parse()
        return Matcher(text.codePoints().toArray(), opcodes, flags)
    }

    enum class Flag {
        Single,
        MultiLine,
        Insensitive,
        Sticky,
        Unicode,
        SingleLine,
        Indices,
    }
}
