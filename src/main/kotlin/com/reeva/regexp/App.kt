package com.reeva.regexp

import com.reeva.regexp.compiler.Executor
import com.reeva.regexp.compiler.convertToNodes

fun main() {
    val ast = Parser("a*aab").parse()
    val (start, end) = ast.convertToNodes()
    val result = Executor(start, end).execute("aaaaaaab")
    println(ast)
}
