package com.reevajs.regexp

class Compiler(private val root: RootNode) {
    private val buffer = GrowableByteBuffer()

    fun compile(): ByteArray {
        compileNode(root)
        return buffer.finalize()
    }

    private fun compileNode(node: ASTNode): Unit = with(buffer) {
        when (node) {
            is RootNode -> node.nodes.forEach(::compileNode)
            is GroupNode -> {
                if (node.index != null) {
                    writeByte(START_GROUP_OP)
                    writeShort(node.index)
                } else writeByte(START_NON_CAPTURING_GROUP_OP)

                node.nodes.forEach(::compileNode)
                writeByte(END_GROUP_OP)
            }
            is CodePointNode -> when {
                node.codePoint <= Byte.MAX_VALUE -> {
                    writeByte(CODEPOINT1_OP)
                    writeByte(node.codePoint.toByte())
                }
                node.codePoint <= Short.MAX_VALUE -> {
                    writeByte(CODEPOINT2_OP)
                    writeShort(node.codePoint.toShort())
                }
                else -> {
                    writeByte(CODEPOINT4_OP)
                    writeInt(node.codePoint)
                }
            }
            is StartNode -> writeByte(START_OP)
            is EndNode -> writeByte(END_OP)
            is AnyNode -> writeByte(ANY_OP)
            is NegateNode -> {
                writeByte(NEGATE_NEXT_OP)
                compileNode(node.node)
            }
            is WordNode -> writeByte(WORD_OP)
            is WordBoundaryNode -> writeByte(WORD_BOUNDARY_OP)
            is DigitNode -> writeByte(DIGIT_OP)
            is WhitespaceNode -> writeByte(WHITESPACE_OP)
            is MatchNode -> writeByte(MATCH_OP)
            is UnicodeClassNode -> {
                writeByte(UNICODE_CLASS_OP)
                expect(node.name.length <= Byte.MAX_VALUE)
                writeByte(node.name.length.toByte())
                for (ch in node.name) {
                    expect(ch.code <= Byte.MAX_VALUE)
                    writeByte(ch.code.toByte())
                }
            }
            is BackReferenceNode -> {
                if (node.index < root.numGroups) {
                    writeByte(BACK_REFERENCE_OP)
                    writeShort(node.index)
                } else if (node.index <= Byte.MAX_VALUE) {
                    writeByte(CODEPOINT1_OP)
                    writeByte(node.index.toByte())
                } else {
                    writeByte(CODEPOINT2_OP)
                    writeShort(node.index)
                }
            }
            is CharacterClassNode -> {
                writeByte(CHAR_CLASS_OP)
                writeShort(node.nodes.size.toShort())
                val opcodes = Compiler(root.copy(nodes = node.nodes)).compile()
                writeShort(opcodes.size.toShort())
                writeBytes(opcodes)
            }
            is InvertedCharacterClassNode -> {
                writeByte(INVERTED_CHAR_CLASS_OP)
                val opcodes = Compiler(root.copy(nodes = node.nodes)).compile()
                writeBytes(opcodes)
            }
            is CodePointRangeNode -> when {
                node.start <= Byte.MAX_VALUE && node.end <= Byte.MAX_VALUE -> {
                    writeByte(RANGE1_OP)
                    writeByte(node.start.toByte())
                    writeByte(node.end.toByte())
                }
                node.start <= Short.MAX_VALUE && node.end <= Short.MAX_VALUE -> {
                    writeByte(RANGE2_OP)
                    writeShort(node.start.toShort())
                    writeShort(node.end.toShort())
                }
                else -> {
                    writeByte(RANGE4_OP)
                    writeInt(node.start)
                    writeInt(node.end)
                }
            }
            is ZeroOrOneNode -> {
                /*
                 * FORK/FORK_NOW X+3
                 * <X bytes>
                 */

                writeByte(if (node.lazy) FORK_NOW_OP else FORK_OP)
                val offset = position
                writeShort(0)

                val count = compileAndGetCount(node.node)

                expect(count + 3 < Short.MAX_VALUE)
                position = offset
                writeShort((count + 3).toShort())
                position = offset + count
            }
            is ZeroOrMoreNode -> {
                /*
                 * FORK/FORK_NOW X+6
                 * <X bytes>
                 * FORK_NOW/FORK -X
                 */

                val firstOp = if (node.lazy) FORK_NOW_OP else FORK_OP
                val secondOp = if (node.lazy) FORK_OP else FORK_NOW_OP

                writeByte(firstOp)
                val offset = position
                writeShort(0)

                val count = compileAndGetCount(node.node)
                expect(count + 6 < Short.MAX_VALUE)
                expect((-count) > Short.MIN_VALUE)
                writeByte(secondOp)
                writeShort((-count).toShort())
                
                val end = position
                position = offset
                writeShort((count + 6).toShort())
                position = end
            }
            is OneOrMoreNode -> {
                /*
                 * <X bytes>
                 * FORK/FORK_NOW -X
                 */

                val count = compileAndGetCount(node.node)
                expect((-count) > Short.MIN_VALUE)
                writeByte(if (node.lazy) FORK_OP else FORK_NOW_OP)
                writeShort((-count).toShort())
            }
            is RepetitionNode -> {
                if (node.min == 0.toShort() && node.max == 0.toShort())
                    return@with

                if (node.min == 1.toShort() && node.max == 1.toShort()) {
                    compileNode(node.node)
                    return@with
                }

                /*
                 * RANGE_JUMP MIN MAX (0 if unbounded) 12 X+15
                 * FORK/FORK_NOW X+6
                 * <X bytes>
                 * JUMP -X-12
                 */

                writeByte(RANGE_JUMP_OP)
                writeShort(node.min)
                writeShort(node.max ?: 0)
                writeShort(12)
                val firstOffsetPos = position
                writeShort(0)

                writeByte(if (node.lazy) FORK_NOW_OP else FORK_OP)
                val secondOffsetPos = position
                writeShort(0)

                val count = compileAndGetCount(node.node)
                expect(count + 15 < Short.MAX_VALUE)
                expect((-count - 12) > Short.MIN_VALUE)

                writeByte(JUMP_OP)
                writeShort((-count - 12).toShort())

                val end = position

                position = firstOffsetPos
                writeShort((count + 15).toShort())

                position = secondOffsetPos
                writeShort((count + 6).toShort())

                position = end
            }
            is AlternationNode -> {
                /*
                 * FORK X+6
                 * <X opcodes>
                 * JUMP Y+3
                 * <Y opcodes>
                 */

                writeByte(FORK_OP)
                val forkOffset = position
                writeShort(0)

                val count1 = compileAndGetCount(node.lhs)
                expect(count1 + 6 < Short.MAX_VALUE)

                writeByte(JUMP_OP)
                val jumpOffset = position
                writeShort(0)

                val count2 = compileAndGetCount(node.rhs)
                expect(count2 + 3 < Short.MAX_VALUE)

                val end = position

                position = forkOffset
                writeShort((count1 + 6).toShort())

                position = jumpOffset
                writeShort((count2 + 3).toShort())

                position = end
            }
            is LookNode -> {
                writeByte(when (node) {
                    is PositiveLookaheadNode -> POSITIVE_LOOKAHEAD_OP
                    is PositiveLookbehindNode -> POSITIVE_LOOKBEHIND_OP
                    is NegativeLookaheadNode -> NEGATIVE_LOOKAHEAD_OP
                    is NegativeLookbehindNode -> NEGATIVE_LOOKBEHIND_OP
                })

                val opcodes = Compiler(root.copy(nodes = node.nodes)).compile()
                expect(opcodes.size < Short.MAX_VALUE)
                writeShort(opcodes.size.toShort())
                writeBytes(opcodes)
            }
        }
    }

    private fun compileAndGetCount(node: ASTNode): Int {
        val start = buffer.position
        compileNode(node)
        return buffer.position - start
    }

    companion object {
        fun compile(rootNode: RootNode): Opcodes {
            return Opcodes(Compiler(rootNode).compile(), rootNode.groupNames)
        }
    }
}
