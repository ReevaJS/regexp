package com.reeva.regexp

const val REGEX = """(\w+)\1"""
const val STRING = "foofoo"

fun main() {
    val result = RegExp(REGEX, RegExp.Flag.Unicode).match(STRING)

    println(result)
}
