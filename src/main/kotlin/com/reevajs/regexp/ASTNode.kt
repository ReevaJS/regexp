package com.reevajs.regexp

sealed class ASTNode

class RootNode(val groupNames: Map<Short, String>, val nodes: List<ASTNode>) : ASTNode()

class GroupNode(val index: Short?, val nodes: List<ASTNode>) : ASTNode()

class CodePointNode(val codePoint: Int) : ASTNode()

data class NegateNode(val node: ASTNode) : ASTNode()

object StartNode : ASTNode()

object EndNode : ASTNode()

object AnyNode : ASTNode()

object WordNode : ASTNode()

object WordBoundaryNode : ASTNode()

object DigitNode : ASTNode()

object WhitespaceNode : ASTNode()

object MatchNode : ASTNode()

class UnicodeClassNode(val name: String) : ASTNode()

// Note: When emitted by the parser, this may not refer to a valid index. If
//       so, the compiler will treat this as if it were a CodepointNode
class BackReferenceNode(var index: Short) : ASTNode()

class CharacterClassNode(val nodes: List<ASTNode>) : ASTNode()

class InvertedCharacterClassNode(val nodes: List<ASTNode>) : ASTNode()

class CodePointRangeNode(val start: Int, val end: Int) : ASTNode()

data class ZeroOrOneNode(val node: ASTNode, val lazy: Boolean) : ASTNode()

data class ZeroOrMoreNode(val node: ASTNode, val lazy: Boolean) : ASTNode()

data class OneOrMoreNode(val node: ASTNode, val lazy: Boolean) : ASTNode()

data class RepetitionNode(val node: ASTNode, val min: Short, val max: Short?, val lazy: Boolean) : ASTNode()

data class AlternationNode(val lhs: ASTNode, val rhs: ASTNode) : ASTNode()

sealed class LookNode(val nodes: List<ASTNode>) : ASTNode()

class PositiveLookaheadNode(nodes: List<ASTNode>) : LookNode(nodes)

class PositiveLookbehindNode(nodes: List<ASTNode>) : LookNode(nodes)

class NegativeLookaheadNode(nodes: List<ASTNode>) : LookNode(nodes)

class NegativeLookbehindNode(nodes: List<ASTNode>) : LookNode(nodes)
