package com.reeva.regexp

fun main() {
    val ast = Parser("abc{4,}d*?(ef)[g-i]{1,3}").parse()
    println(ast)
}
