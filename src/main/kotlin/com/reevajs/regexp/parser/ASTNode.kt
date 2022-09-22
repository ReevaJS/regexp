package com.reevajs.regexp.parser

sealed class ASTNode {
    open fun asReversed() = this
}

data class RootNode(val groupNames: Map<Short, String>, val numGroups: Short, val nodes: List<ASTNode>) : ASTNode() {
    override fun asReversed() = RootNode(groupNames, numGroups, nodes.asReversed().map(ASTNode::asReversed))
}

data class GroupNode(val index: Short?, val nodes: List<ASTNode>) : ASTNode() {
    override fun asReversed() = GroupNode(index, nodes.asReversed().map(ASTNode::asReversed))
}

data class CodePointNode(val codePoint: Int) : ASTNode()

data class CodePointListNode(val codePoints: ByteArray) : ASTNode()

data class NegateNode(val node: ASTNode) : ASTNode() {
    override fun asReversed() = NegateNode(node.asReversed())
}

object StartNode : ASTNode() {
    override fun toString() = "StartNode"
}

object EndNode : ASTNode() {
    override fun toString() = "EndNode"
}

object AnyNode : ASTNode() {
    override fun toString() = "AnyNode"
}

object WordNode : ASTNode() {
    override fun toString() = "WordNode"
}

object WordBoundaryNode : ASTNode() {
    override fun toString() = "WordBoundaryNode"
}

object DigitNode : ASTNode() {
    override fun toString() = "DigitNode"
}

object WhitespaceNode : ASTNode() {
    override fun toString() = "WhitespaceNode"
}

object MatchNode : ASTNode() {
    override fun toString() = "MatchNode"
}

data class UnicodeClassNode(val name: String) : ASTNode()

// Note: When emitted by the parser, this may not refer to a valid index. If
//       so, the compiler will treat this as if it were a CodepointNode
data class BackReferenceNode(var index: Short) : ASTNode()

// TODO: Does this need an asReversed impl? It should only match one character anyways
data class CharacterClassNode(val nodes: List<ASTNode>) : ASTNode() 

data class InvertedCharacterClassNode(val nodes: List<ASTNode>) : ASTNode()

data class CodePointRangeNode(val start: Int, val end: Int) : ASTNode()

interface QuantifierNode

data class ZeroOrOneNode(val node: ASTNode, val lazy: Boolean) : ASTNode(), QuantifierNode {
    override fun asReversed() = ZeroOrOneNode(node.asReversed(), lazy)
}

data class ZeroOrMoreNode(val node: ASTNode, val lazy: Boolean) : ASTNode(), QuantifierNode {
    override fun asReversed() = ZeroOrMoreNode(node.asReversed(), lazy)
}

data class OneOrMoreNode(val node: ASTNode, val lazy: Boolean) : ASTNode(), QuantifierNode {
    override fun asReversed() = OneOrMoreNode(node.asReversed(), lazy)
}

data class RepetitionNode(val node: ASTNode, val min: Short, val max: Short?, val lazy: Boolean) : ASTNode(), QuantifierNode {
    override fun asReversed() = RepetitionNode(node.asReversed(), min, max, lazy)
}

data class AlternationNode(val lhs: ASTNode, val rhs: ASTNode) : ASTNode() {
    override fun asReversed() = AlternationNode(lhs.asReversed(), rhs.asReversed())
}

sealed class LookNode(val nodes: List<ASTNode>) : ASTNode()

class PositiveLookaheadNode(nodes: List<ASTNode>) : LookNode(nodes) {
    override fun asReversed() = PositiveLookaheadNode(nodes.asReversed().map(ASTNode::asReversed))
} 

class PositiveLookbehindNode(nodes: List<ASTNode>) : LookNode(nodes) {
    override fun asReversed() = PositiveLookbehindNode(nodes.asReversed().map(ASTNode::asReversed))
}

class NegativeLookaheadNode(nodes: List<ASTNode>) : LookNode(nodes) {
    override fun asReversed() = NegativeLookaheadNode(nodes.asReversed().map(ASTNode::asReversed))
}

class NegativeLookbehindNode(nodes: List<ASTNode>) : LookNode(nodes) {
    override fun asReversed() = NegativeLookbehindNode(nodes.asReversed().map(ASTNode::asReversed))
}
