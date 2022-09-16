package com.reeva.regexp

class RegExp(
    private val regexCodePoints: IntArray, 
    private val flags: Set<Flag>,
) {
    constructor(regex: String, vararg flags: Flag) : this(regex.codePoints().toArray(), flags.toSet()) 

    fun test(text: String) = match(text) != null

    fun match(text: String) = matcher(text).match()

    fun matcher(text: String): Matcher {
        val opcodes = Parser(regexCodePoints, unicode = Flag.Unicode in flags).parse()

        for (op in opcodes)
            println(op)
        println()

        return Matcher(text.codePoints().toArray(), opcodes, flags)
    }

    enum class Flag {
        MultiLine,
        Insensitive,
        Unicode,
        SingleLine,
    }
}
