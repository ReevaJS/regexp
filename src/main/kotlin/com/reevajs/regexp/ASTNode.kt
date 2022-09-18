package com.reevajs.regexp

sealed class ASTNode

class RootNode(val groupNames: Map<Int, String>, val nodes: List<ASTNode>) : ASTNode()

class GroupNode(val index: Int?, val nodes: List<ASTNode>) : ASTNode()

class CodePointNode(val codepoint: Int) : ASTNode()

class CodePointRangeNode(val start: Int, val end: Int) : ASTNode()

class CharacterClassNode(val nodes: List<ASTNode>) : ASTNode()

class InvertedCharacterClassNode(val nodes: List<ASTNode>) : ASTNode()

class StartGroupNode(val index: Int?) : ASTNode()

object EndGroupNode : ASTNode()

object NegateNextNode : ASTNode()

object WordNode : ASTNode()

object WordBoundaryNode : ASTNode()

object DigitNode : ASTNode()

object WhitespaceNode : ASTNode()

class UnicodeClassNode(val name: String) : ASTNode()

// Note: When emitted by the parser, this may not refer to a valid index. If
//       so, the compiler will treat this as if it were a CodepointNode
class BackReferenceNode(var index: Int) : ASTNode()

object StartNode : ASTNode()

object EndNode : ASTNode()

object AnyNode : ASTNode()

object MatchNode : ASTNode()

data class ZeroOrOneNode(val node: ASTNode, val lazy: Boolean) : ASTNode()

data class ZeroOrMoreNode(val node: ASTNode, val lazy: Boolean) : ASTNode()

data class OneOrMoreNode(val node: ASTNode, val lazy: Boolean) : ASTNode()

data class RepetitionNode(val node: ASTNode, val min: Int, val max: Int?, val lazy: Boolean) : ASTNode()

data class AlternationNode(val lhs: ASTNode, val rhs: ASTNode) : ASTNode()

data class PositiveLookaheadNode(val nodes: List<ASTNode>) : ASTNode()

data class PositiveLookbehindNode(val nodes: List<ASTNode>) : ASTNode()

data class NegativeLookaheadNode(val nodes: List<ASTNode>) : ASTNode()

data class NegativeLookbehindNode(val nodes: List<ASTNode>) : ASTNode()
