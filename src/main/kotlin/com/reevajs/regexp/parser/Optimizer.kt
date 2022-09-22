package com.reevajs.regexp.parser

object Optimizer {
    @Suppress("UNCHECKED_CAST")
    fun <T : ASTNode> optimize(node: T): T = when (node) {
        is RootNode -> RootNode(node.groupNames, node.numGroups, optimizeChildren(node.nodes))
        is GroupNode -> GroupNode(node.index, optimizeChildren(node.nodes))
        is NegateNode -> NegateNode(optimize(node.node))
        is CharacterClassNode -> CharacterClassNode(optimizeChildren(node.nodes))
        is InvertedCharacterClassNode -> InvertedCharacterClassNode(optimizeChildren(node.nodes))
        is ZeroOrOneNode -> ZeroOrOneNode(optimize(node.node), node.lazy)
        is ZeroOrMoreNode -> ZeroOrMoreNode(optimize(node.node), node.lazy)
        is OneOrMoreNode -> OneOrMoreNode(optimize(node.node), node.lazy)
        is RepetitionNode -> RepetitionNode(optimize(node.node), node.min, node.max, node.lazy)
        is AlternationNode -> AlternationNode(optimize(node.lhs), optimize(node.rhs))
        is PositiveLookaheadNode -> PositiveLookaheadNode(optimizeChildren(node.nodes))
        is PositiveLookbehindNode -> PositiveLookbehindNode(optimizeChildren(node.nodes))
        is NegativeLookaheadNode -> NegativeLookaheadNode(optimizeChildren(node.nodes))
        is NegativeLookbehindNode -> NegativeLookbehindNode(optimizeChildren(node.nodes))
        else -> node
    } as T

    private fun optimizeChildren(nodes: List<ASTNode>): List<ASTNode> {
        val newNodes = mutableListOf<ASTNode>()
        val runningCodePointBytes = mutableListOf<Byte>()

        for (node in nodes) {
            if (node is CodePointNode && node.codePoint <= Byte.MAX_VALUE) {
                if (runningCodePointBytes.size == Byte.MAX_VALUE.toInt()) {
                    newNodes.add(CodePointListNode(runningCodePointBytes.toByteArray()))
                    runningCodePointBytes.clear()
                }

                runningCodePointBytes.add(node.codePoint.toByte())
            } else {
                if (runningCodePointBytes.size >= 2) {
                    newNodes.add(CodePointListNode(runningCodePointBytes.toByteArray()))
                    runningCodePointBytes.clear()
                } else if (runningCodePointBytes.size == 1) {
                    newNodes.add(CodePointNode(runningCodePointBytes[0].toInt()))
                    runningCodePointBytes.clear()
                }

                newNodes.add(node)
            }
        }

        return newNodes
    }
}
