package com.reeva.regexp

import java.time.OffsetDateTime

sealed class Opcode

// Everything is stored as an int, since storing it as a Char does _actually_
// save any space (all integer types except Long are reduced to Int on the JVM)
class CharOp(val codepoint: Int) : Opcode()

class CharArrayOp(val codepoints: IntArray) : Opcode()

// Used for character classes
class CharRangeOp(val start: Int, val end: Int) : Opcode()

class StartGroupOp(val index: Int) : Opcode()

object EndGroupOp : Opcode()

object NegateNextOp : Opcode()

object WordOp : Opcode()

object WordBoundaryOp : Opcode()

object DigitOp : Opcode()

object WhitespaceOp : Opcode()

class BackReferenceOp(val index: Int) : Opcode()

////////////////////
// VM-focused ops //
////////////////////

// Opcodes which may need an offset adjusted if opcodes get inserted
// arbitrarily into the opcode list
sealed class OffsetOpcode(var offset: Int) : Opcode()

class Fork(offset: Int) : OffsetOpcode(offset)

class Jump(offset: Int) : OffsetOpcode(offset)

object Match : Opcode()
