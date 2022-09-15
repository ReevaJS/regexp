package com.reeva.regexp

fun main() {
    val opcodes = Parser("aa*?ab", unicode = false).parse()

    opcodes.forEach {
        println(it)
    }
}
