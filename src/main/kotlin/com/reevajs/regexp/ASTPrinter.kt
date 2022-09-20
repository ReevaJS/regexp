package com.reevajs.regexp

class ASTPrinter(private var indent: Int = 0) {
    fun print(node: ASTNode) {
        printIndent()
        print(node::class.simpleName!!)

        when (node) {
            is RootNode -> printChildren(node.nodes)
            is GroupNode -> {
                print(" ${node.index?.toString() ?: ""}")
                printChildren(node.nodes)
            }
            is CodePointNode -> {
                if (node.codePoint <= Char.MAX_VALUE.code) {
                    println(" ${node.codePoint.toChar()}")
                } else {
                    println(" 0x${node.codePoint.toString(radix = 16)}")
                }
            }
            is NegateNode -> {
                println()
                indented { print(node.node) }
            }
            is UnicodeClassNode -> println(" \"${node.name}\"")
            is BackReferenceNode -> println(" ${node.index}")
            is CharacterClassNode -> printChildren(node.nodes)
            is InvertedCharacterClassNode -> printChildren(node.nodes)
            is CodePointRangeNode -> {
                val start = node.start.toInt().let {
                    if (it < Char.MAX_VALUE.code) it.toChar().toString() else it.toString(radix = 16)
                }
                val end = node.end.toInt().let {
                    if (it < Char.MAX_VALUE.code) it.toChar().toString() else it.toString(radix = 16)
                }
                println(" $start..$end")
            }
            is ZeroOrOneNode -> {
                println(" ${if (node.lazy) "lazy" else ""}")
                indented { print(node.node) }
            }
            is ZeroOrMoreNode -> {
                println(" ${if (node.lazy) "lazy" else ""}")
                indented { print(node.node) }
            }
            is OneOrMoreNode -> {
                println(" ${if (node.lazy) "lazy" else ""}")
                indented { print(node.node) }
            }
            is RepetitionNode -> {
                val min = node.min.toInt().let {
                    if (it < Char.MAX_VALUE.code) it.toChar().toString() else it.toString(radix = 16)
                }
                val max = node.max?.toInt()?.let {
                    if (it < Char.MAX_VALUE.code) it.toChar().toString() else it.toString(radix = 16)
                } ?: "∞"
                println(" ${if (node.lazy) "lazy" else ""} $min..$max")
            }
            is AlternationNode -> {
                println()
                indented {
                    print(node.lhs)
                    print(node.rhs)
                }
            }
            is LookNode -> printChildren(node.nodes)
            else -> println()
        }
    }

    private fun printChildren(childNodes: List<ASTNode>) = indented {
        println()
        childNodes.forEach(::print)
    }

    private fun indented(block: () -> Unit) {
        indent += 1
        block()
        indent -= 1
    }

    private fun printIndent() {
        print("  ".repeat(indent))
    }
}
