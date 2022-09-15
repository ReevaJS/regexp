package com.reeva.regexp

sealed class Opcode

// Everything is stored as an int, since storing it as a Char does _actually_
// save any space (all integer types except Long are reduced to Int on the JVM)
class CharOp(val codepoint: Int) : Opcode() {
    override fun toString() = if (codepoint < Char.MAX_VALUE.code) {
        "CharOp(${codepoint.toChar()})"
    } else "CharOp($codepoint)"
}

data class CharClassOp(val numEntries: Int) : Opcode()

// Used for character classes
data class CharRangeOp(val start: Int, val end: Int) : Opcode()

data class StartGroupOp(val index: Int) : Opcode()

object EndGroupOp : Opcode()

object NegateNextOp : Opcode()

object WordOp : Opcode()

object WordBoundaryOp : Opcode()

object DigitOp : Opcode()

object WhitespaceOp : Opcode()

data class BackReferenceOp(val index: Int) : Opcode()

object StartOp : Opcode()

object EndOp : Opcode()

object AnyOp : Opcode()

////////////////////
// VM-focused ops //
////////////////////

// Opcodes which may need an offset adjusted if opcodes get inserted
// arbitrarily into the opcode list
sealed class OffsetOpcode(var offset: Int) : Opcode()

// Clone the current state and branch into a different opcode. Keep
// executing the current state
class Fork(offset: Int) : OffsetOpcode(offset) {
    override fun toString() = "Fork($offset)"
}

// Clone the current state and branch into a different opcode. Start
// executing the fork immediately
class ForkNow(offset: Int) : OffsetOpcode(offset) {
    override fun toString() = "ForkNow($offset)"
}

class Jump(offset: Int) : OffsetOpcode(offset) {
    override fun toString() = "Jump($offset)"
}

object Match : Opcode()
