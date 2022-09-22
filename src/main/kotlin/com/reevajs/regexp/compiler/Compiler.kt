package com.reevajs.regexp.compiler

import com.reevajs.regexp.parser.*
import com.reevajs.regexp.expect
import com.reevajs.regexp.unreachable

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
            is CodePointListNode -> {
                writeByte(CODEPOINT1_LIST_OP)
                writeByte(node.codePoints.size.toByte())
                node.codePoints.forEach(::writeByte)
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
                 * FORK/FORK_NOW _END
                 * <X bytes>
                 * END_:
                 */

                val end = Label()
                writeJump(if (node.lazy) FORK_NOW_OP else FORK_OP, end)
                compileNode(node.node)
                placeLabel(end)
            }
            is ZeroOrMoreNode -> {
                /*
                 * FORK/FORK_NOW _END
                 * _BYTES:
                 * <X bytes>
                 * FORK_NOW/FORK _BYTES
                 * _END:
                 */

                val firstOp = if (node.lazy) FORK_NOW_OP else FORK_OP
                val secondOp = if (node.lazy) FORK_OP else FORK_NOW_OP

                val bytesLabel = Label()
                val endLabel = Label()

                writeJump(firstOp, endLabel)
                placeLabel(bytesLabel)

                compileNode(node.node)

                writeJump(secondOp, bytesLabel)
                placeLabel(endLabel)
            }
            is OneOrMoreNode -> {
                /*
                 * _BYTES:
                 * <X bytes>
                 * FORK/FORK_NOW _BYTES
                 */

                val label = Label()
                placeLabel(label)
                compileNode(node.node)
                writeJump(if (node.lazy) FORK_OP else FORK_NOW_OP, label)
            }
            is RepetitionNode -> {
                if (node.min == 0.toShort() && node.max == 0.toShort())
                    return@with

                if (node.min == 1.toShort() && node.max == 1.toShort()) {
                    compileNode(node.node)
                    return@with
                }

                /*
                 * _START:
                 * RANGE_JUMP MIN MAX _BYTES _END
                 * FORK/FORK_NOW _END
                 * _BYTES:
                 * <X bytes>
                 * JUMP _START
                 * _END:
                 */

                val startLabel = Label()
                val bytesLabel = Label()
                val endLabel = Label()

                placeLabel(startLabel)
                writeByte(RANGE_JUMP_OP)
                writeShort(node.min)
                writeShort(node.max ?: 0)
                writeJumpRef(bytesLabel)
                writeJumpRef(endLabel)
                
                writeJump(if (node.lazy) FORK_NOW_OP else FORK_OP, endLabel)
                placeLabel(bytesLabel)

                compileNode(node.node)
                writeJump(JUMP_OP, startLabel)

                placeLabel(endLabel)
            }
            is AlternationNode -> {
                /*
                 * FORK _RHS
                 * _LHS:
                 * <X opcodes>
                 * JUMP _END
                 * _RHS:
                 * <Y opcodes>
                 * _END
                 */

                val lhsLabel = Label()
                val rhsLabel = Label()
                val endLabel = Label()

                writeJump(FORK_OP, rhsLabel)
                placeLabel(lhsLabel)
                compileNode(node.lhs)
                writeJump(JUMP_OP, endLabel)
                placeLabel(rhsLabel)
                compileNode(node.rhs)
                placeLabel(endLabel)
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

    private fun writeJump(op: Byte, label: Label) {
        buffer.writeByte(op)
        writeJumpRef(label)
    }

    private fun writeJumpRef(label: Label) {
        if (label.position != null) {
            val offset = label.position!! - buffer.position
            expect(offset in Short.MIN_VALUE..Short.MAX_VALUE)
            buffer.writeShort(offset.toShort())
        } else {
            label.offsetsToWritePositionTo.add(buffer.position)
            buffer.writeShort(0)
        }
    }

    private fun placeLabel(label: Label) {
        label.position = buffer.position

        for (offset in label.offsetsToWritePositionTo)
            buffer.buffer.putShort(offset, (buffer.position - offset).toShort())
    }

    class Label {
        val offsetsToWritePositionTo = mutableListOf<Int>()
        var position: Int? = null
    }

    companion object {
        fun compile(rootNode: RootNode): Opcodes {
            return Opcodes(Compiler(rootNode).compile(), rootNode.groupNames)
        }
    }
}
