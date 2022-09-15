package com.reeva.regexp

object ASTOptimizer {
    fun optimize(ast: AST): AST {
        return when (ast) {
            is RootAST -> RootAST(optimize(ast.group) as GroupAST)
            is GroupAST -> {
                val children = mutableListOf<AST>()

                for (child in ast.children) {
                    val last = children.lastOrNull()
                    if (last == null) {
                        children.add(child)
                        continue
                    }

                    if (last is CharAST && child is CharAST) {
                        children.removeLast()
                        children.add(StringAST("${last.char}${child.char}"))
                    } else if (last is StringAST && child is CharAST) {
                        children.removeLast()
                        children.add(StringAST("${last.string}${child.char}"))
                    } else {
                        children.add(child)
                    }
                }

                GroupAST(children)
            }
            is CharacterClassAST -> {
                val ranges = mutableListOf<CharacterRange>(ast.ranges.first())

                for (range in ast.ranges.drop(1)) {
                    val last = ranges.last()

                    when {
                        range in last -> continue
                        range.start == last.end + 1 -> {
                            ranges.removeLast()
                            ranges.add(CharacterRange(last.start, range.end))
                        }
                        range.end + 1 == last.start -> {
                            ranges.removeLast()
                            ranges.add(CharacterRange(range.start, last.end))
                        }
                        else -> ranges.add(range)
                    }
                }

                CharacterClassAST(ranges, ast.inverted)
            }
            is RepetitionAST -> if (ast.amount == 0) DeadAST else {
                ast.copy(target = optimize(ast.target))
            }
            is RepetitionRangeAST -> if (ast.lowerBound == ast.upperBound) {
                RepetitionAST(ast.target, ast.lowerBound)
            } else {
                ast.copy(target = optimize(ast.target))
            }
            is AlternationAST -> {
                val options = mutableListOf<AST>()

                for (option in ast.options) {
                    val optimized = optimize(option)
                    if (optimized is AlternationAST) {
                        options.addAll(optimized.options)
                    } else {
                        options.add(optimized)
                    }
                }

                AlternationAST(options)
            }
            else -> ast
        }
    }
}
