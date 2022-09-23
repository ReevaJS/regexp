package com.reevajs.regexp.compiler

class Opcodes(
    val bytes: ByteArray,
    val groupNames: Map<Short, String>,
)

/**
 * START_GROUP_OP
 * index: SHORT
 */
const val START_GROUP_OP: Byte = 1

const val START_NON_CAPTURING_GROUP_OP: Byte = 2
const val END_GROUP_OP: Byte = 3

/**
 * CODEPOINT1_OP
 * BYTE
 */
const val CODEPOINT1_OP: Byte = 4

/**
 * CODEPOINT2_OP
 * SHORT
 */
const val CODEPOINT2_OP: Byte = 5

/**
 * CODEPOINT4_OP
 * INT
 */
const val CODEPOINT4_OP: Byte = 6

/**
 * CODEPOINT1_LIST_OP
 * num codepoints: BYTE
 * <num codepoints> BYTE
 * 
 * Note that this variant only exists for code point values under 256 as that
 * is the common case. Matching against short/int-ranged code points will be
 * quite rare, and this is only a memory optimization anyways
 */
const val CODEPOINT1_LIST_OP: Byte = 7

const val START_OP: Byte = 8
const val END_OP: Byte = 9
const val ANY_OP: Byte = 10
const val NEGATE_NEXT_OP: Byte = 11
const val WORD_OP: Byte = 12
const val WORD_BOUNDARY_OP: Byte = 13
const val DIGIT_OP: Byte = 14
const val WHITESPACE_OP: Byte = 15
const val MATCH_OP: Byte = 16

/**
 * UNICODE_CLASS_OP
 * length: BYTE
 * class name: <length> BYTE (all ASCII, as all unicode class names are normal letters)
 */
const val UNICODE_CLASS_OP: Byte = 17

/**
 * BACK_REFERENCE_OP
 * index: SHORT
 */
const val BACK_REFERENCE_OP: Byte = 18

/**
 * CHAR_CLASS_OP
 * num entries: SHORT
 * offset: SHORT
 */
const val CHAR_CLASS_OP: Byte = 19

/**
 * INVERTED_CHAR_CLASS_OP
 * num entries: SHORT
 * num bytes: SHORT
 */
const val INVERTED_CHAR_CLASS_OP: Byte = 20

/**
 * RANGE1_OP
 * start: BYTE
 * end: BYTE
 */
const val RANGE1_OP: Byte = 21

/**
 * RANGE2_OP
 * start: SHORT
 * end: SHORT
 */
const val RANGE2_OP: Byte = 22

/**
 * RANGE4_OP
 * start: INT
 * end: INT
 */
const val RANGE4_OP: Byte = 23

////////////////////
// VM-focused ops //
////////////////////

// Note: All offset are relative to the byte corresponding to the opcode to which they belong. 
// In other words, "JUMP 0" would be an infinite loop

/**
 * Clone the current state and branch into a different opcode. Keep
 * executing the current state
 *
 * FORK_OP
 * offset: SHORT
 */
const val FORK_OP: Byte = 24

/**
 * Clone the current state and branch into a different opcode. Start
 * executing the fork immediately
 *
 * FORK_NOW_OP
 * offset: SHORT
 */
const val FORK_NOW_OP: Byte = 25

/**
 * JUMP_OP
 * offset: SHORT
 */
const val JUMP_OP: Byte = 26

/**
 * Saves the current position for a later REPEAT op to ensure we do not loop if our target
 * opcodes matched an empty string.
 *
 * SAVE_POS_OP
 * index: SHORT
 */
const val SAVE_POS_OP: Byte = 27

/**
 * Like FORK_OP, but only forks if the current position is different from the one associated
 * with index.
 *
 * REPEAT_FORK_OP
 * offset: SHORT
 * index: SHORT
 */
const val REPEAT_FORK_OP: Byte = 28

/**
 * Like FORK_NOW_OP, but only forks if the current position is different from the one associated
 * with index.
 *
 * REPEAT_FORK_NOW_OP
 * offset: SHORT
 * index: SHORT
 */
const val REPEAT_FORK_NOW_OP: Byte = 29

/**
 * Like JUMP_OP, but only jumps if the current position is different from the one associated
 * with index.
 *
 * REPEAT_FORK_NOW_OP
 * offset: SHORT
 * index: SHORT
 */
const val REPEAT_JUMP_OP: Byte = 30

/**
 * RANGE_JUMP_OP
 * min: SHORT
 * max: SHORT
 * below offset: SHORT
 * above offset: SHORT
 */
const val RANGE_JUMP_OP: Byte = 31

/**
 * POSITIVE_LOOKAHEAD_OP
 * num opcodes: SHORT
 * <num opcodes>
 */
const val POSITIVE_LOOKAHEAD_OP: Byte = 32

/**
 * POSITIVE_LOOKBEHIND_OP
 * num opcodes: SHORT
 * <num opcodes>
 */
const val POSITIVE_LOOKBEHIND_OP: Byte = 33

/**
 * NEGATIVE_LOOKAHEAD_OP
 * num opcodes: SHORT
 * <num opcodes>
 */
const val NEGATIVE_LOOKAHEAD_OP: Byte = 34

/**
 * NEGATIVE_LOOKBEHIND_OP
 * num opcodes: SHORT
 * <num opcodes>
 */
const val NEGATIVE_LOOKBEHIND_OP: Byte = 35
