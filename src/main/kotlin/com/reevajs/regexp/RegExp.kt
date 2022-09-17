
package com.reevajs.regexp

@Suppress("unused", "MemberVisibilityCanBePrivate")
class RegExp(
    regexCodePoints: IntArray,
    private val flags: Set<Flag>,
) {
    private val opcodes = Parser(regexCodePoints, unicode = Flag.Unicode in flags).parse()

    constructor(regex: String, vararg flags: Flag) : this(regex.codePoints().toArray(), flags.toSet()) 

    fun test(codePoints: IntArray) = match(codePoints) != null

    fun test(text: String) = test(text.codePoints().toArray())

    fun match(codePoints: IntArray) = matcher(codePoints).matchAll()

    fun match(text: String) = match(text.codePoints().toArray())

    fun matcher(codePoints: IntArray): Matcher {
        return Matcher(codePoints, opcodes, flags)
    }

    fun matcher(text: String) = matcher(text.codePoints().toArray())

    enum class Flag {
        MultiLine,
        Insensitive,
        Unicode,
        DotMatchesNewlines,
    }
}

fun String.toRegExp(vararg flags: RegExp.Flag) =
    RegExp(codePoints().toArray(), flags.toSet())
