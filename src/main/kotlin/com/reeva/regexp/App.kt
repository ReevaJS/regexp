package com.reeva.regexp

const val REGEX = "aa*?a$"
const val STRING = "aaaaaa"

fun main() {
    /* 
     * StartGroupOp(0)
     * CharOp(a)
     * ForkNow(3)
     * CharOp(a)
     * Fork(-1)
     * CharOp(a)
     * CharOp(b)
     * EndGroupOp
     * MatchOp
     * 
     *  a a a a a a b 
     *   ^
     */

    val opcodes = Parser(REGEX, unicode = false).parse()

    opcodes.forEach {
        println(it)
    }

    val result = Matcher(STRING.codePoints().toArray(), opcodes).match()

    println(result)
}
