package com.reeva.regexp

class MatchGroup(
    val codepoints: IntArray,
    val range: IntRange,
) {
    val value = buildString {
        codepoints.forEach(::appendCodePoint)
    }

    fun copy() = MatchGroup(codepoints.copyOf(), range)

    override fun toString() = "MatchGroup(\"$value\", range=$range)"
}

data class MatchResult(
    val groups: List<MatchGroup>,
    val namedGroups: Map<String, MatchGroup>,
) {
    val groupValues: List<String>
        get() = groups.map { it.value }
}

class Matcher(
    private val source: IntArray, 
    private val opcodes: Array<Opcode>,
    private val flags: Set<RegExp.Flag>,
) {
    private val pendingStates = mutableListOf<MatchState>()
    
    fun match(startIndex: Int = 0): List<MatchResult>? {
        pendingStates.clear()

        // Common-case optimization: Don't check the entire string if regex starts with
        // a StartOp (it will be the second op, since the first is always StartGroupOp(0))

        if (opcodes[1] == StartOp)
            return if (startIndex != 0) null else exec(MatchState(0, 0))?.let(::listOf)

        var sourceIndex = startIndex
        val results = mutableListOf<MatchResult>()

        while (sourceIndex < source.size) {
            val result = exec(MatchState(sourceIndex, 0))
            if (result != null) {
                results.add(result)
                sourceIndex = result.groups[0].range.last
            } else {
                sourceIndex++
            }
        }

        return if (results.isEmpty()) null else results
    }

    private fun exec(initialState: MatchState): MatchResult? {
        var state = initialState

        while (true) {
            when (execOp(state)) {
                ExecResult.Match -> {
                    val indexedGroups = state.groupContents.toList()
                        .filter { it.first is Int }                        
                        .sortedBy { it.first as Int }
                        .map { it.second }
                    
                    @Suppress("UNCHECKED_CAST")
                    val namedGroups = state.groupContents.toList()
                        .filter { it.first is String }
                        .toMap() as Map<String, MatchGroup>

                    return MatchResult(indexedGroups, namedGroups)
                }
                ExecResult.Continue -> {}
                ExecResult.Fail -> {
                    if (pendingStates.isEmpty())
                        return null
                    state = pendingStates.removeLast()
                }
            }
        }
    }

    private fun execOp(state: MatchState, op: Opcode = state.op): ExecResult {
        println("State: $state, op: $op")
        return when (op) {
            is StartGroupOp -> {
                state.groups.add(GroupState(op.index, state.sourceCursor))
                state.advanceOp()
                ExecResult.Continue
            }
            is StartNamedGroupOp -> {
                state.groups.add(GroupState(op.name, state.sourceCursor))
                state.advanceOp()
                ExecResult.Continue
            }
            is EndGroupOp -> {
                val groupState = state.groups.removeLast()
                if (groupState.key != null) {
                    expect(groupState.key !in state.groupContents)
                    state.groupContents[groupState.key] = MatchGroup(
                        groupState.content.toIntArray(),
                        groupState.rangeStart until state.sourceCursor,
                    )
                }
                state.advanceOp()
                ExecResult.Continue
            }
            is CharOp -> {
                if (!state.done && state.codepoint == op.codepoint) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            is CharClassOp -> {
                val start = state.opcodeCursor
                state.advanceOp()

                for (i in 0 until op.numEntries) {
                    if (execOp(state, opcodes[state.opcodeCursor + i]) == ExecResult.Continue) {
                        state.opcodeCursor = start + op.numEntries
                        return ExecResult.Continue
                    }
                }

                return ExecResult.Fail
            }
            is CharRangeOp -> {
                if (!state.done && state.codepoint in op.start..op.end) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            NegateNextOp -> {
                state.advanceOp()
                when (execOp(state)) {
                    ExecResult.Match -> unreachable()
                    ExecResult.Continue -> ExecResult.Fail
                    ExecResult.Fail -> ExecResult.Continue
                }
            }
            WordOp -> {
                if (!state.done && isWordCodepoint(state.codepoint)) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            WordBoundaryOp -> {
                val lastIsWord = if (state.sourceCursor != 0) {
                    isWordCodepoint(source[state.sourceCursor - 1])
                } else false

                val currentIsWord = !state.done && isWordCodepoint(state.codepoint)

                if (lastIsWord != currentIsWord) {
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            DigitOp -> {
                if (!state.done && state.codepoint in 0x30..0x39 /* 0-9 */) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            WhitespaceOp -> {
                if (!state.done && isWhitespaceCodepoint(state.codepoint)) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            is BackReferenceOp -> {
                val content = state.groupContents[op.index]?.codepoints ?: TODO()

                val startCursor = state.sourceCursor
                if (startCursor + content.size > source.size)
                    return ExecResult.Fail

                for (i in content.indices) {
                    if (content[i] != source[startCursor + i])
                        return ExecResult.Fail
                }

                state.advanceOp()
                state.advanceSource(content.size)
                ExecResult.Continue
            }
            StartOp -> {
                if (state.sourceCursor == 0) {
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            EndOp -> {
                if (state.sourceCursor == source.size) {
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            AnyOp -> {
                if (state.sourceCursor < source.size) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            MatchOp -> ExecResult.Match
            is Fork -> {
                val newState = state.copy()
                newState.opcodeCursor += op.offset
                pendingStates.add(newState)
                state.advanceOp()
                ExecResult.Continue
            }
            is ForkNow -> {
                val newState = state.copy()
                newState.advanceOp()
                pendingStates.add(newState)
                state.opcodeCursor += op.offset
                ExecResult.Continue
            }
            is Jump -> {
                state.opcodeCursor += op.offset + 1
                ExecResult.Continue
            }
            is LookOp -> {
                state.advanceOp()

                var newState = MatchState(state.sourceCursor, 0)

                val matched = if (op.isAhead) {
                    Matcher(source, op.opcodes, flags).exec(newState) != null
                } else {
                    var matched = false

                    // TODO: Determine opcode min/max length here
                    for (newPos in state.sourceCursor downTo 0) {
                        newState = MatchState(newPos, 0)

                        if (Matcher(source, op.opcodes, flags).exec(newState) != null && newState.sourceCursor == state.sourceCursor) {
                            matched = true
                            break
                        }
                    }

                    matched
                }

                if (op.isPositive != matched) {
                    ExecResult.Fail
                } else {
                    // Save any new capturing groups
                    for ((key, value) in newState.groupContents) {
                        if (key == 0) // Skip the implicit group
                            continue

                        if (key !in state.groupContents)
                            state.groupContents[key] = value
                    }

                    ExecResult.Continue
                }
            }
        }
    }

    private fun isWordCodepoint(cp: Int): Boolean =
        cp in 0x41..0x5a /*A-Z*/ || cp in 0x61..0x7a /*a-z*/ || cp in 0x30..0x39 /*0-9*/ || cp == 0x5f /*_*/

    // TODO: Are these the only characters? Does unicode mode change this?
    private fun isWhitespaceCodepoint(cp: Int): Boolean =
        cp == 0x20 /* <space> */ || cp == 0x9 /* <tab> */ || cp == 0xa /* <new line> */ || cp == 0xd /* <carriage return> */

    private enum class ExecResult {
        Match,
        Continue,
        Fail,
    }

    private data class GroupState(
        val key: Any? /* Int | String | null (non-capturing) */,
        var rangeStart: Int,
        var content: MutableList<Int> = mutableListOf(),
    ) {
        fun copy() = GroupState(key, rangeStart, content.toMutableList())
    }

    private val MatchState.done: Boolean
        get() = sourceCursor > source.lastIndex

    private val MatchState.codepoint: Int
        get() = source[sourceCursor]

    private val MatchState.op: Opcode
        get() = opcodes[opcodeCursor]

    private fun MatchState.advanceSource(n: Int = 1) {
        repeat(n) {
            groups.forEach {
                it.content.add(source[sourceCursor])
            }
            sourceCursor++
        }
    }
    
    private class MatchState(
        var sourceCursor: Int,
        var opcodeCursor: Int,
        val groups: MutableList<GroupState> = mutableListOf(),
        val groupContents: MutableMap<Any /* Int | String */, MatchGroup> = mutableMapOf(),
    ) {
        fun advanceOp() {
            opcodeCursor++
        }

        fun copy() = MatchState(
            sourceCursor,
            opcodeCursor,
            groups.map { it.copy() }.toMutableList(),
            groupContents.mapValues { (_, v) -> v.copy() }.toMutableMap(),
        )

        override fun toString() = "MatchState(SP=$sourceCursor, OP=$opcodeCursor)"
    }
}
