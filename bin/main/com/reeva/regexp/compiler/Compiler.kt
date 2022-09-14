package com.reeva.regexp.compiler

import com.reeva.regexp.AST
import com.reeva.regexp.RootAST

data class NodePair(val start: Node, val end: Node)

fun RootAST.convertToNodes(): NodePair {
    val start = Node()
    val end = Node()

    val groupNode = generateNode(group, end)
    start.addTransition(EpsilonTransition, groupNode)

    return NodePair(start, end)
}

private fun generateNode(ast: AST, nextNode: Node): Node {
    TODO()
    // when (ast) {
    //     is GroupAST -> {

    //     }
    // }
}
