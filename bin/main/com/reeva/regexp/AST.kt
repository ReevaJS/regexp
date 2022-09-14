package com.reeva.regexp

sealed class AST

object StartAST : AST()

object EndAST : AST()

object AnyAST : AST()

// Used in optimization to indicate something that can't be matched
object DeadAST : AST() 

data class RootAST(val group: GroupAST) : AST()

data class GroupAST(val children: List<AST>) : AST()

data class CharAST(val char: Char) : AST()

data class StringAST(val string: String) : AST()

data class CharacterRange(val start: Char, val end: Char) : AST() {
    operator fun contains(other: CharacterRange): Boolean {
        return start <= other.start && end >= other.end
    }
}

data class CharacterClassAST(val ranges: List<CharacterRange>, val inverted: Boolean) : AST()

data class RepetitionAST(val target: AST, val amount: Int) : AST()

data class RepetitionRangeAST(
    val target: AST, 
    val lowerBound: Int, 
    val upperBound: Int?, 
    val lazy: Boolean,
) : AST()

data class AlternationAST(val options: List<AST>) : AST()

data class WordBoundaryAST(val inverted: Boolean) : AST()

data class WhitespaceAST(val inverted: Boolean) : AST()

data class DigitAST(val inverted: Boolean) : AST()

data class WordAST(val inverted: Boolean) : AST()
