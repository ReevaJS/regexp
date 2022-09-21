package com.reevajs.regexp

import java.nio.ByteBuffer

class OpcodePrinter(private val opcodes: Opcodes, private val indent: Int = 0) {
    fun print() {
        var i = 0
        val bytes = ByteBuffer.wrap(opcodes.bytes)
        val lenWidth = opcodes.bytes.size.toString().length

        println("raw: ${opcodes.bytes.joinToString(separator = " ")}")

        while (bytes.hasRemaining()) {
            print("${" ".repeat(indent)}%${lenWidth}d: ".format(bytes.position()))

            when (val op = bytes.get()) {
                START_GROUP_OP -> println("START_GROUP ${bytes.short}")
                START_NON_CAPTURING_GROUP_OP -> println("START_NON_CAPTURING_GROUP")
                END_GROUP_OP -> println("END_GROUP")
                CODEPOINT1_OP -> println("CODEPOINT1 ${bytes.get().toInt().toChar()}")
                CODEPOINT2_OP -> println("CODEPOINT2 ${bytes.short.toInt().toChar()}")
                CODEPOINT4_OP -> println("CODEPOINT4 ${bytes.int}")
                START_OP -> println("START")
                END_OP -> println("END")
                ANY_OP -> println("ANY")
                NEGATE_NEXT_OP -> println("NEGATE_NEXT")
                WORD_OP -> println("WORD")
                WORD_BOUNDARY_OP -> println("WORD_BOUNDARY")
                DIGIT_OP -> println("DIGIT")
                WHITESPACE_OP -> println("WHITESPACE")
                MATCH_OP -> println("MATCH")
                UNICODE_CLASS_OP -> {
                    val len = bytes.get().toInt()
                    val clazz = buildString {
                        repeat(len) {
                            append(bytes.get().toInt().toChar())
                        }
                    }
                    println("UNICODE_CLASS $clazz")
                }
                BACK_REFERENCE_OP -> {
                    val index = bytes.short
                    val name = opcodes.groupNames[index]
                    print("BACK_REFERENCE $index")
                    if (name != null) {
                        println(" ($name)")
                    } else {
                        println()
                    }
                }
                CHAR_CLASS_OP -> println("CHAR_CLASS entries=${bytes.short} offset=${bytes.short}")
                INVERTED_CHAR_CLASS_OP -> println("INVERTED_CHAR_CLASS entries=${bytes.short} offset=${bytes.short}")
                RANGE1_OP -> println("RANGE1 ${bytes.get()}..${bytes.get()}")
                RANGE2_OP -> println("RANGE2 ${bytes.short}..${bytes.short}")
                RANGE4_OP -> println("RANGE4 ${bytes.int}..${bytes.int}")
                FORK_OP -> println("FORK ${bytes.short}")
                FORK_NOW_OP -> println("FORK_NOW ${bytes.short}")
                JUMP_OP -> println("JUMP ${bytes.short}")
                RANGE_JUMP_OP -> println("RANGE_CHECK ${bytes.short}..${bytes.short} below=${bytes.short} above=${bytes.short}")
                in POSITIVE_LOOKAHEAD_OP..NEGATIVE_LOOKBEHIND_OP -> {
                    val numOpcodes = bytes.short.toInt()
                    val array = ByteArray(numOpcodes)
                    bytes.get(array)
                    i += numOpcodes

                    print(when (op) {
                        POSITIVE_LOOKAHEAD_OP -> "POSITIVE_LOOKAHEAD"
                        POSITIVE_LOOKBEHIND_OP -> "POSITIVE_LOOKBEHIND"
                        NEGATIVE_LOOKAHEAD_OP -> "NEGATIVE_LOOKAHEAD"
                        NEGATIVE_LOOKBEHIND_OP -> "NEGATIVE_LOOKBEHIND"
                        else -> unreachable()
                    })

                    println(" $numOpcodes")

                    OpcodePrinter(Opcodes(array, opcodes.groupNames), indent + 4).print()
                }
                else -> unreachable()
            }

            i++
        }
    }
}
