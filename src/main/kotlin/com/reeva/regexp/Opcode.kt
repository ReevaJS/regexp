package com.reeva.regexp

sealed class Opcode

// Everything is stored as an int, since storing it as a Char does _actually_
// save any space (all integer types except Long are reduced to Int on the JVM)
class CharOp(val codePoint: Int) : Opcode() {
    override fun toString() = if (codePoint < Char.MAX_VALUE.code) {
        "Char(${codePoint.toChar()})"
    } else "Char($codePoint)"
}

// Used for character classes
class CharRangeOp(val start: Int, val end: Int) : Opcode()  {
    override fun toString() = "CharRange($start-$end)"
}

// Null index indicates non-capturing group. Index cannot be null if name is not null
class StartGroupOp(val index: Int?, val name: String?) : Opcode() {
    init {
        if (name != null)
            expect(index != null)
    }

    override fun toString() = "StartGroup($index, $name)"
}

object EndGroupOp : Opcode() {
    override fun toString() = "EndGroup"
}

object NegateNextOp : Opcode() {
    override fun toString() = "NegateNext"
}

object WordOp : Opcode() {
    override fun toString() = "Word"
}

object WordBoundaryOp : Opcode() {
    override fun toString() = "WordBoundary"
}

object DigitOp : Opcode() {
    override fun toString() = "Digit"
}

object WhitespaceOp : Opcode() {
    override fun toString() = "Whitespace"
}

data class UnicodeClassOp(val class_: String) : Opcode() {
    override fun toString() = "UnicodeClass($class_)"
}

data class BackReferenceOp(val key: Any /* String | Int */) : Opcode() {
    override fun toString() = "BackReferenceOp(\\$key)"
}

object StartOp : Opcode() {
    override fun toString() = "Start"
}

object EndOp : Opcode() {
    override fun toString() = "End"
}

object AnyOp : Opcode() {
    override fun toString() = "Any"
}

// Completes the RegExp match
object MatchOp : Opcode() {
    override fun toString() = "Match"
}

class CharClassOp(val numEntries: Int, offset: Int) : OffsetOpcode(offset) {
    override fun toString() = "CharClass($numEntries)"
}

class InvertedCharClassOp(val numEntries: Int, offset: Int) : OffsetOpcode(offset) {
    override fun toString() = "InvertedCharClass($numEntries)"
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

class RangeCheck(val min: Int, val max: Int?) : Opcode() {
    override fun toString() = "RangeCheck($min, $max)"
}

class JumpIfBelowRange(offset: Int) : OffsetOpcode(offset) {
    override fun toString() = "JumpIfBelowRange($offset)"
}

class JumpIfAboveRange(offset: Int) : OffsetOpcode(offset) {
    override fun toString() = "JumpIfAboveRange($offset)"
}

sealed class LookOp(val opcodes: Array<Opcode>, val isPositive: Boolean, val isAhead: Boolean) : Opcode() {
    override fun toString() = buildString {
        append(this@LookOp::class.simpleName!!.replace("Op", ""))
        append(" [\n")
        opcodes.forEach { 
            append("  ")
            append(it)
            append('\n')
        }
        append("]")
    }
}

class PositiveLookaheadOp(opcodes: Array<Opcode>) : LookOp(opcodes, true, true)

class NegativeLookaheadOp(opcodes: Array<Opcode>) : LookOp(opcodes, false, true)

class PositiveLookbehindOp(opcodes: Array<Opcode>) : LookOp(opcodes, true, false)

class NegativeLookbehindOp(opcodes: Array<Opcode>) : LookOp(opcodes, false, false)
