package com.reeva.regexp

sealed class Opcode

// Everything is stored as an int, since storing it as a Char does _actually_
// save any space (all integer types except Long are reduced to Int on the JVM)
class CharOp(val codepoint: Int) : Opcode() {
    override fun toString() = if (codepoint < Char.MAX_VALUE.code) {
        "CharOp(${codepoint.toChar()})"
    } else "CharOp($codepoint)"
}

class CharClassOp(val numEntries: Int) : Opcode() {
    override fun toString() = "CharClassOp($numEntries)"
}

// Used for character classes
class CharRangeOp(val start: Int, val end: Int) : Opcode()  {
    override fun toString() = "CharRangeOp($start-$end)"
}

class StartGroupOp(val index: Int) : Opcode() {
    override fun toString() = "StartGroupOp($index)"
}

object EndGroupOp : Opcode() {
    override fun toString() = "EndGroupOp"
}

object NegateNextOp : Opcode() {
    override fun toString() = "NegateNextOp"
}

object WordOp : Opcode() {
    override fun toString() = "WordOp"
}

object WordBoundaryOp : Opcode() {
    override fun toString() = "WordBoundaryOp"
}

object DigitOp : Opcode() {
    override fun toString() = "DigitOp"
}

object WhitespaceOp : Opcode() {
    override fun toString() = "WhitespaceOp"
}

data class BackReferenceOp(val index: Int) : Opcode() {
    override fun toString() = "BackReferenceOp(\\$index)"
}

object StartOp : Opcode() {
    override fun toString() = "StartOp"
}

object EndOp : Opcode() {
    override fun toString() = "EndOp"
}

object AnyOp : Opcode() {
    override fun toString() = "AnyOp"
}

// Completes the RegExp match
object MatchOp : Opcode() {
    override fun toString() = "MatchOp"
}

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
