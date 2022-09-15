package com.reeva.regexp

data class MatchResult(
    val groups: List<String>,
    val namedGroups: Map<String, String>,
)

class Matcher(private val source: IntArray, private val opcodes: List<Opcode>) {
    private val pendingStates = mutableListOf<MatchState>()
    private var shouldNegateNext = false

    fun match(): MatchResult? {
        return exec(MatchState(0, 0))
    }

    private fun exec(initialState: MatchState): MatchResult? {
        var state = initialState

        while (true) {
            when (execOp(state)) {
                ExecResult.Match -> {
                    // We shouldn't be able to end the regex inside of a group
                    expect(state.groups.isEmpty())

                    val indexedGroups = state.groupContents.toList()
                        .filter { it.first is Int }                        
                        .sortedBy { it.first as Int } as List<Pair<Int, IntArray>>

                    indexedGroups.forEachIndexed { index, value ->
                        expect(index == value.first)
                    }

                    val namedGroups = state.groupContents.toList()
                        .filter { it.first is String }
                        .toMap() as Map<String, IntArray>

                    return MatchResult(
                        groups = indexedGroups.map { (_, groupCPs) ->
                            buildString { groupCPs.forEach(::appendCodePoint) }
                        },
                        namedGroups = namedGroups.mapValues { (_, groupCPs) ->
                            buildString { groupCPs.forEach(::appendCodePoint) }
                        },
                    )
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
                state.groups.add(GroupState(op.index))
                state.advanceOp()
                ExecResult.Continue
            }
            is StartNamedGroupOp -> {
                state.groups.add(GroupState(op.name))
                state.advanceOp()
                ExecResult.Continue
            }
            is EndGroupOp -> {
                val groupState = state.groups.removeLast()
                if (groupState.key != null) {
                    expect(groupState.key !in state.groupContents)
                    state.groupContents[groupState.key] = groupState.content.toIntArray()
                }
                state.advanceOp()
                ExecResult.Continue
            }
            is CharOp -> {
                if (state.codepoint == op.codepoint) {
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
                if (state.codepoint in op.start..op.end) {
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
                if (isWordCodepoint(state.codepoint)) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            WordBoundaryOp -> {
                val lastIsWord = if (state.sourceCursor != 0) {
                    isWordCodepoint(source[state.sourceCursor - 1])
                } else false

                val currentIsWord = isWordCodepoint(state.codepoint)

                if (lastIsWord != currentIsWord) {
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            DigitOp -> {
                if (state.codepoint in 0x30..0x39 /* 0-9 */) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            WhitespaceOp -> {
                if (isWhitespaceCodepoint(state.codepoint)) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            is BackReferenceOp -> {
                val content = state.groupContents[op.index]
                if (content == null)
                    TODO() // Is this actually possible?

                val startCursor = state.sourceCursor
                for (i in 0 until content.size) {
                    if (content[i] != source[startCursor + i])
                        return ExecResult.Fail
                }

                state.advanceOp()
                state.sourceCursor += content.size
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
        }
    }

    private fun isWordCodepoint(cp: Int): Boolean =
        cp in 0x41..0x5a /*A-Z*/ || cp in 0x6a..0x7a /*a-z*/ || cp in 0x30..0x39 /*0-9*/ || cp == 0x5f /*_*/

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
        var content: MutableList<Int> = mutableListOf(),
    ) {
        fun copy() = GroupState(key, content.toMutableList())
    }

    private val MatchState.codepoint: Int
        inline get() = source[sourceCursor]

    private val MatchState.op: Opcode
        inline get() = opcodes[opcodeCursor]

    private fun MatchState.advanceSource() {
        groups.forEach {
            it.content.add(source[sourceCursor])
        }
        sourceCursor++
    }
    
    private class MatchState(
        var sourceCursor: Int,
        var opcodeCursor: Int,
        val groups: MutableList<GroupState> = mutableListOf(),
        val groupContents: MutableMap<Any /* Int | String */, IntArray> = mutableMapOf(),
    ) {
        fun advanceOp() {
            opcodeCursor++
        }

        fun copy() = MatchState(
            sourceCursor,
            opcodeCursor,
            groups.map { it.copy() }.toMutableList(),
            groupContents.mapValues { (_, v) -> v.copyOf() }.toMutableMap(),
        )

        override fun toString() = "MatchState(SP=$sourceCursor, OP=$opcodeCursor)"
    }
}
