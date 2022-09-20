package com.reevajs.regexp

sealed class ASTNode

data class RootNode(val groupNames: Map<Short, String>, val nodes: List<ASTNode>) : ASTNode()

data class GroupNode(val index: Short?, val nodes: List<ASTNode>) : ASTNode()

data class CodePointNode(val codePoint: Int) : ASTNode()

data class NegateNode(val node: ASTNode) : ASTNode()

object StartNode : ASTNode()

object EndNode : ASTNode()

object AnyNode : ASTNode()

object WordNode : ASTNode()

object WordBoundaryNode : ASTNode()

object DigitNode : ASTNode()

object WhitespaceNode : ASTNode()

object MatchNode : ASTNode()

data class UnicodeClassNode(val name: String) : ASTNode()

// Note: When emitted by the parser, this may not refer to a valid index. If
//       so, the compiler will treat this as if it were a CodepointNode
data class BackReferenceNode(var index: Short) : ASTNode()

data class CharacterClassNode(val nodes: List<ASTNode>) : ASTNode()

data class InvertedCharacterClassNode(val nodes: List<ASTNode>) : ASTNode()

data class CodePointRangeNode(val start: Int, val end: Int) : ASTNode()

interface QuantifierNode

data class ZeroOrOneNode(val node: ASTNode, val lazy: Boolean) : ASTNode(), QuantifierNode

data class ZeroOrMoreNode(val node: ASTNode, val lazy: Boolean) : ASTNode(), QuantifierNode

data class OneOrMoreNode(val node: ASTNode, val lazy: Boolean) : ASTNode(), QuantifierNode

data class RepetitionNode(val node: ASTNode, val min: Short, val max: Short?, val lazy: Boolean) : ASTNode(), QuantifierNode

data class AlternationNode(val lhs: ASTNode, val rhs: ASTNode) : ASTNode()

sealed class LookNode(val nodes: List<ASTNode>) : ASTNode()

class PositiveLookaheadNode(nodes: List<ASTNode>) : LookNode(nodes)

class PositiveLookbehindNode(nodes: List<ASTNode>) : LookNode(nodes)

class NegativeLookaheadNode(nodes: List<ASTNode>) : LookNode(nodes)

class NegativeLookbehindNode(nodes: List<ASTNode>) : LookNode(nodes)
