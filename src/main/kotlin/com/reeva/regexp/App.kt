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

    val result = RegExp(REGEX, RegExp.Flag.Unicode).match(STRING)

    println(result)
}
