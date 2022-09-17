package com.reeva.regexp

import com.ibm.icu.text.UnicodeSet

@Suppress("MemberVisibilityCanBePrivate")
class Matcher(
    private val source: IntArray, 
    private val opcodes: Array<Opcode>,
    private val flags: Set<RegExp.Flag>,
) {
    private val pendingStates = mutableListOf<MatchState>()
    private var negateNext = false
    private val rangeCounts = mutableMapOf<Int, Int>()
    private var rangeResult: RangeResult? = null

    fun matchSequence(startIndex: Int = 0): Sequence<MatchResult> = sequence {
        var index = startIndex

        while (index < source.size) {
            val result = match(index++) ?: continue
            yield(result)
            index = result.indexedGroups[0].range.last + 1
        }
    }

    fun matchAll(startIndex: Int = 0): List<MatchResult>? {
        return matchSequence(startIndex).toList().takeIf { it.isNotEmpty() }
    }

    fun match(startIndex: Int = 0): MatchResult? {
        pendingStates.clear()
        return exec(MatchState(startIndex, 0))
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
        return when (op) {
            is StartGroupOp -> {
                state.groups.add(GroupState(op.index, op.name, state.sourceCursor))
                state.advanceOp()
                ExecResult.Continue
            }
            is EndGroupOp -> {
                val groupState = state.groups.removeLast()
                if (groupState.index != null) {
                    val group = MatchGroup(
                        groupState.content.toIntArray(),
                        groupState.rangeStart until state.sourceCursor,
                    )
                    state.groupContents[groupState.index] = group
                    if (groupState.name != null)
                        state.groupContents[groupState.name] = group
                }
                state.advanceOp()
                ExecResult.Continue
            }
            is CharOp -> {
                if (checkCondition(!state.done && state.codePoint == op.codePoint)) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            is CharClassOp -> {
                state.advanceOp()
                val start = state.opcodeCursor

                for (i in 0 until op.numEntries) {
                    if (execOp(state, opcodes[state.opcodeCursor + i]) == ExecResult.Continue) {
                        state.opcodeCursor = start + op.numEntries
                        return ExecResult.Continue
                    }
                }

                return ExecResult.Fail
            }
            is InvertedCharClassOp -> {
                state.advanceOp()

                expect(!negateNext)

                for (i in 0 until op.numEntries) {
                    if (execOp(state.copy(), opcodes[state.opcodeCursor + i]) == ExecResult.Continue) {
                        return ExecResult.Fail
                    }
                }

                state.opcodeCursor += op.numEntries
                state.advanceSource()

                return ExecResult.Continue
            }
            is CharRangeOp -> {
                if (checkCondition(!state.done && state.codePoint in op.start..op.end)) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            NegateNextOp -> {
                state.advanceOp()
                negateNext = true
                execOp(state).also {
                    negateNext = false
                }
            }
            WordOp -> {
                if (checkCondition(!state.done && isWordCodepoint(state.codePoint))) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            WordBoundaryOp -> {
                val lastIsWord = if (state.sourceCursor != 0) {
                    isWordCodepoint(source[state.sourceCursor - 1])
                } else false

                val currentIsWord = !state.done && isWordCodepoint(state.codePoint)

                if (checkCondition(lastIsWord != currentIsWord)) {
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            DigitOp -> {
                if (checkCondition(!state.done && state.codePoint in 0x30..0x39 /* 0-9 */)) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            WhitespaceOp -> {
                if (checkCondition(!state.done && isWhitespaceCodepoint(state.codePoint))) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            is UnicodeClassOp -> {
                val set = unicodeSets.getOrPut(op.class_) {
                    UnicodeSet("[\\p{${op.class_}}]").freeze()
                }

                if (checkCondition(!state.done && state.codePoint in set)) {
                    state.advanceSource()
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            is BackReferenceOp -> {
                // Take the following regexp as an example: "\k<a>(?<a>x)"
                // This regex will match "x", meaning that if this group doesn't exist, we should
                // consume nothing and return Continue
                val content = state.groupContents[op.key]?.codePoints ?: run {
                    state.advanceOp()
                    return if (checkCondition(true)) ExecResult.Continue else ExecResult.Fail
                }

                val startCursor = state.sourceCursor

                val matched = run {
                    if (startCursor + content.size > source.size)
                        return@run false

                    for (i in content.indices) {
                        if (content[i] != source[startCursor + i])
                            return@run false
                    }

                    true
                }

                if (checkCondition(matched)) {
                    state.advanceOp()
                    state.advanceSource(content.size)
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            StartOp -> {
                if (checkCondition(state.sourceCursor == 0)) {
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            EndOp -> {
                if (checkCondition(state.sourceCursor == source.size)) {
                    state.advanceOp()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            AnyOp -> {
                if (checkCondition(state.sourceCursor < source.size)) {
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
            is RangeCheck -> {
                state.advanceOp()
                val currentValue = rangeCounts.getOrPut(state.opcodeCursor) { 0 }
                
                rangeResult = when {
                    currentValue < op.min -> RangeResult.Below
                    op.max != null && currentValue > op.max -> RangeResult.Above
                    else -> RangeResult.In
                }

                rangeCounts[state.opcodeCursor] = currentValue + 1
                
                ExecResult.Continue
            }
            is JumpIfBelowRange -> {
                state.advanceOp()
                if (rangeResult == RangeResult.Below)
                state.opcodeCursor += op.offset
                ExecResult.Continue
            }
            is JumpIfAboveRange -> {
                state.advanceOp()
                if (rangeResult == RangeResult.Above)
                    state.opcodeCursor += op.offset
                ExecResult.Continue
            }
            is LookOp -> {
                expect(!negateNext)
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

    // Simple function that makes the intent a bit more clear. Easier to read than (a == b) != negateNext
    private fun checkCondition(condition: Boolean) = condition != negateNext

    private enum class ExecResult {
        Match,
        Continue,
        Fail,
    }

    private data class GroupState(
        val index: Int?,
        val name: String?,
        var rangeStart: Int,
        var content: MutableList<Int> = mutableListOf(),
    ) {
        fun copy() = GroupState(index, name, rangeStart, content.toMutableList())
    }

    private val MatchState.done: Boolean
        get() = sourceCursor > source.lastIndex

    private val MatchState.codePoint: Int
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
        val groupContents: MutableMap<Any, MatchGroup> = mutableMapOf(),
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

    private enum class RangeResult {
        Below,
        In,
        Above
    }

    companion object {
        private val unicodeSets = mutableMapOf<String, UnicodeSet>()
    }
}
