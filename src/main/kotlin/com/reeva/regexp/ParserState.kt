package com.reeva.regexp

internal class ParserState {
    private val opcodes = mutableListOf<Opcode>()
    private val marks = mutableListOf<Mark>()
    var alternationMark: ParserState.Mark = mark()
    var modifierMark: ParserState.Mark = mark()

    val size: Int
        get() = opcodes.size

    fun mark(offset: Int = opcodes.size) = Mark(offset)

    fun build() = opcodes.toTypedArray()

    fun addOpcode(opcode: Opcode) {
        opcodes.add(opcode)
    }

    fun addOpcode(index: Int, opcode: Opcode) {
        // Make any necessary adjustments to existing opcodes/marks
        for ((opIndex, op) in opcodes.withIndex()) {
            if (op !is OffsetOpcode)
                continue

            // Jump 4   <-- opcode with offset past the new opcode
            // Char a
            // Char b   <-- new opcode inserted at index 2
            // Char c
            if (opIndex < index && op.offset >= index)
                op.offset++

            // Char a
            // Char b   <-- new opcode inserted at index 1
            // Char c
            // Jump -2   <-- opcode with offset before the new opcode
            if (opIndex >= index && opIndex + op.offset < index)
                op.offset--
        }

        marks.forEach { mark ->
            if (mark.offset >= index)
                mark.offset++
        }

        // Add new opcode/mark
        opcodes.add(index, opcode)
    }

    fun merge(other: ParserState) {
        other.marks.forEach {
            val newOffset = it.offset + opcodes.size
            it.offset = newOffset
            marks.add(it)
        }
        this.opcodes.addAll(other.opcodes)
    }

    inner class Mark(var offset: Int) {
        fun opcode() = opcodes[offset]

        fun next() = marks[offset + 1]

        fun previous() = marks[offset - 1]

        fun insertBefore(opcode: Opcode) = addOpcode(offset, opcode)

        fun insertAfter(opcode: Opcode) = addOpcode(offset + 1, opcode)

        fun replace(opcode: Opcode) {
            opcodes[offset] = opcode
        }

        override fun toString() = "Mark($offset)"
    }
}
