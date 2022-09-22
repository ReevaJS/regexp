package com.reevajs.regexp

import com.reevajs.regexp.compiler.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.max

@Suppress("MemberVisibilityCanBePrivate")
class Matcher(
    private val source: IntArray,
    opcodes: Opcodes,
    private val flags: Set<RegExp.Flag>,
) {
    private val buffer = ByteBuffer.wrap(opcodes.bytes)
    private val groupNames = opcodes.groupNames

    private val pendingStates = mutableListOf<MatchState>()
    private var negateNext = false
    private val rangeCounts = mutableMapOf<Int, Int>()

    private val dotMatchesNewlines = RegExp.Flag.DotMatchesNewlines in flags
    private val unicode = RegExp.Flag.Unicode in flags

    fun matchSequence(startIndex: Int = 0): Sequence<MatchResult> = sequence {
        var index = startIndex

        while (index <= source.size) {
            val result = match(index)
            if (result != null) {
                yield(result)
                index = max(index + 1, result.groups[0]!!.range.last + 1)
            } else {
                index++
            }
        }
    }

    fun matchAll(startIndex: Int = 0): List<MatchResult>? {
        return matchSequence(startIndex).toList().takeIf { it.isNotEmpty() }
    }

    fun match(startIndex: Int = 0): MatchResult? {
        pendingStates.clear()
        rangeCounts.clear()
        return exec(MatchState(startIndex, 0))
    }

    private fun exec(initialState: MatchState): MatchResult? {
        var state = initialState

        // Set up the implicit group 0 state
        state.groups.add(GroupState(0, state.sourceCursor))

        while (true) {
            when (execOp(state)) {
                ExecResult.Match -> {
                    // Store the implicit group 0 state
                    val group0 = state.groups.removeLast()
                    expect(group0.index == 0.toShort())
                    expect(state.groups.isEmpty())
                    state.groupContents[0] = MatchGroup(
                        group0.content.toIntArray(),
                        group0.rangeStart until state.sourceCursor,
                    )

                    return MatchResult(
                        TreeMap(state.groupContents.mapKeys { (k, _) -> k.toInt() }),
                        groupNames.mapKeys { (k, _) -> k.toInt() },
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

    private fun execOp(state: MatchState, position: Int = state.opcodeCursor): ExecResult = with(state) {
        if (state.sourceCursor > source.size)
            return ExecResult.Fail

        buffer.position(position)

        val op = readByte()
        return when (op) {
            START_GROUP_OP -> {
                groups.add(GroupState(readShort(), sourceCursor))
                ExecResult.Continue
            }
            START_NON_CAPTURING_GROUP_OP -> {
                groups.add(GroupState(null, sourceCursor))
                ExecResult.Continue
            }
            END_GROUP_OP -> {
                val groupState = groups.removeLast()
                if (groupState.index != null) {
                    groupContents[groupState.index] = MatchGroup(
                        groupState.content.toIntArray(),
                        groupState.rangeStart until sourceCursor,
                    )
                }
                ExecResult.Continue
            }
            CODEPOINT1_OP -> {
                val cp = readByte().toInt()
                if (!done && checkCondition(codePoint == cp)) {
                    advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            CODEPOINT2_OP -> {
                val cp = readShort().toInt()
                if (!done && checkCondition(codePoint == cp)) {
                    advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            CODEPOINT4_OP -> {
                val cp = readInt()
                if (!done && checkCondition(codePoint == cp)) {
                    advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            CODEPOINT1_LIST_OP -> {
                val length = readByte().toInt()
                if (state.sourceCursor + length > source.size)
                    return ExecResult.Fail

                val codePoints = readBytes(length)
                val result = run {
                    for ((index, codePoint) in codePoints.withIndex()) {
                        if (codePoint.toInt() != source[sourceCursor + index])
                            return@run false    
                    }

                    true
                }

                if (checkCondition(result)) {
                    advanceSource(length)
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            START_OP -> {
                if (checkCondition(sourceCursor == 0)) {
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            END_OP -> {
                if (checkCondition(sourceCursor == source.size)) {
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            ANY_OP -> {
                val result = run {
                    if (done)
                        return@run false

                    if (!dotMatchesNewlines && isLineSeparator(codePoint))
                        return@run false

                    if (!unicode && codePoint > Char.MAX_VALUE.code)
                        return@run false

                    true
                }

                if (checkCondition(result)) {
                    advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            NEGATE_NEXT_OP -> {
                negateNext = true
                execOp(state).also {
                    negateNext = false
                }
            }
            WORD_OP -> {
                if (!done && checkCondition(isWordCodepoint(codePoint))) {
                    advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            WORD_BOUNDARY_OP -> {
                val lastIsWord = if (sourceCursor != 0) {
                    isWordCodepoint(source[sourceCursor - 1])
                } else false

                val currentIsWord = !done && isWordCodepoint(codePoint)

                if (checkCondition(lastIsWord != currentIsWord)) {
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            DIGIT_OP -> {
                if (!done && checkCondition(codePoint in '0'.code..'9'.code)) {
                    advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            WHITESPACE_OP -> {
                if (!done && checkCondition(isWhitespace(codePoint))) {
                    advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            MATCH_OP -> ExecResult.Match
            UNICODE_CLASS_OP -> {
                val length = readByte().toInt()
                val clazz = buildString {
                    repeat(length) {
                        append(readByte().toInt().toChar())
                    }
                }
                val set = getUnicodeClass(clazz)

                if (!done && checkCondition(codePoint in set)) {
                    advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            BACK_REFERENCE_OP -> {
                // Take the following regexp as an example: "\k<a>(?<a>x)"
                // This regex will match "x", meaning that if this group doesn't exist, we should
                // consume nothing and return Continue
                val content = state.groupContents[readShort()]?.codePoints ?: run {
                    return if (checkCondition(true)) ExecResult.Continue else ExecResult.Fail
                }

                val startCursor = sourceCursor

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
                    advanceSource(content.size)
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            CHAR_CLASS_OP -> {
                val numEntries = readShort()
                val numBytes = readShort()

                val start = opcodeCursor

                for (i in 0 until numEntries) {
                    if (execOp(state, opcodeCursor + i) == ExecResult.Continue) {
                        opcodeCursor = start + numBytes.toInt()
                        return ExecResult.Continue
                    }
                }

                return ExecResult.Fail
            }
            INVERTED_CHAR_CLASS_OP -> {
                if (done)
                    return ExecResult.Fail

                expect(!negateNext)

                val numEntries = readShort()
                val offset = readShort()

                for (i in 0 until numEntries) {
                    if (execOp(state.copy(), opcodeCursor + i) == ExecResult.Continue)
                        return ExecResult.Fail
                }

                opcodeCursor += offset
                advanceSource()

                return ExecResult.Continue
            }
            RANGE1_OP -> {
                val start = readByte().toInt()
                val end = readByte().toInt()
                if (!done && checkCondition(codePoint in start..end)) {
                    advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            RANGE2_OP -> {
                val start = readShort().toInt()
                val end = readShort().toInt()
                if (!done && checkCondition(codePoint in start..end)) {
                    advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            RANGE4_OP -> {
                val start = readInt()
                val end = readInt()
                if (!done && checkCondition(codePoint in start..end)) {
                    state.advanceSource()
                    ExecResult.Continue
                } else ExecResult.Fail
            }
            FORK_OP -> {
                val newState = state.copy()
                newState.opcodeCursor += readShort()
                pendingStates.add(newState)
                ExecResult.Continue
            }
            FORK_NOW_OP -> {
                val offset = readShort() - 2 // the offset is relative to itself
                val newState = state.copy()
                pendingStates.add(newState)
                opcodeCursor += offset
                ExecResult.Continue
            }
            JUMP_OP -> {
                opcodeCursor += readShort()
                ExecResult.Continue
            }
            RANGE_JUMP_OP -> {
                val opOffset = state.opcodeCursor - 1

                val min = readShort().toInt()
                val max = readShort().toInt().takeIf { it != 0 }
                val belowOffset = opcodeCursor + readShort().toInt()
                val aboveOffset = opcodeCursor + readShort().toInt()

                val currentValue = rangeCounts.getOrPut(opOffset) { 0 }

                when {
                    currentValue < min -> opcodeCursor = belowOffset
                    max != null && currentValue >= max -> opcodeCursor = aboveOffset
                }

                rangeCounts[opOffset] = currentValue + 1

                ExecResult.Continue
            }
            // Note: Don't use `in` here. The Kotlin compiler can't see through it,
            //       and won't generate a TABLESWITCH for this `when` statement.
            POSITIVE_LOOKAHEAD_OP,
            POSITIVE_LOOKBEHIND_OP,
            NEGATIVE_LOOKAHEAD_OP,
            NEGATIVE_LOOKBEHIND_OP -> {
                expect(!negateNext)

                val opcodes = Opcodes(readBytes(readShort().toInt()), groupNames)

                val isAhead = op == POSITIVE_LOOKAHEAD_OP || op == NEGATIVE_LOOKAHEAD_OP
                val isPositive = op == POSITIVE_LOOKAHEAD_OP || op == POSITIVE_LOOKBEHIND_OP

                val match = if (isAhead) {
                    Matcher(source, opcodes, flags).match(state.sourceCursor)
                } else {
                    val reversedSource = source.reversedArray()
                    val reversedCursor = source.size - sourceCursor
                    Matcher(reversedSource, opcodes, flags).match(reversedCursor)
                }

                if (isPositive != (match != null)) {
                    ExecResult.Fail
                } else {
                    // Save any new capturing groups
                    for ((key, value) in match?.groups.orEmpty()) {
                        if (key == 0) // Skip the implicit group
                            continue

                        if (key.toShort() !in state.groupContents)
                            state.groupContents[key.toShort()] = value.let {
                                if (isAhead) it else MatchGroup(
                                    it.codePoints.reversedArray(),
                                    (source.lastIndex - it.range.last)..(source.lastIndex - it.range.first)
                                )
                            }
                    }

                    ExecResult.Continue
                }
            }
            else -> {
                println("unknown op $op at offset ${opcodeCursor - 1}")
                unreachable()
            }
        }
    }

    private fun isWordCodepoint(cp: Int): Boolean =
        cp in 'A'.code..'Z'.code || cp in 'a'.code..'z'.code || cp in '0'.code..'9'.code || cp == '_'.code

    // Simple function that makes the intent a bit more clear. Easier to read than (a == b) != negateNext
    private fun checkCondition(condition: Boolean) = condition != negateNext

    private enum class ExecResult {
        Match,
        Continue,
        Fail,
    }

    private data class GroupState(
        val index: Short?,
        var rangeStart: Int,
        var content: MutableList<Int> = mutableListOf(),
    ) {
        fun copy() = GroupState(index, rangeStart, content.toMutableList())
    }

    private val MatchState.done: Boolean
        get() = sourceCursor !in source.indices

    private val MatchState.codePoint: Int
        get() = source[sourceCursor]

    private fun MatchState.advanceSource(n: Int = 1) {
        repeat(n) {
            groups.forEach {
                it.content.add(source[sourceCursor])
            }
            sourceCursor++
        }
    }

    private fun MatchState.peekByte() = buffer.get(opcodeCursor)
    private fun MatchState.peekShort() = buffer.getShort(opcodeCursor)
    private fun MatchState.peekInt() = buffer.getInt(opcodeCursor)

    private fun MatchState.readByte() = buffer.get(opcodeCursor).also {
        opcodeCursor += 1
    }

    private fun MatchState.readShort() = buffer.getShort(opcodeCursor).also {
        opcodeCursor += 2
    }

    private fun MatchState.readInt() = buffer.getInt(opcodeCursor).also {
        opcodeCursor += 4
    }

    private fun MatchState.readBytes(count: Int): ByteArray {
        val array = ByteArray(count)
        val prevPos = buffer.position()
        buffer.position(opcodeCursor)
        buffer.get(array)
        opcodeCursor += count
        buffer.position(prevPos)
        return array
    }

    private class MatchState(
        var sourceCursor: Int,
        var opcodeCursor: Int,
        val groups: MutableList<GroupState> = mutableListOf(),
        val groupContents: MutableMap<Short, MatchGroup> = mutableMapOf(),
    ) {
        fun copy() = MatchState(
            sourceCursor,
            opcodeCursor,
            groups.map { it.copy() }.toMutableList(),
            groupContents.mapValues { (_, v) -> v.copy() }.toMutableMap(),
        )

        override fun toString() = "MatchState(SP=$sourceCursor, OP=$opcodeCursor)"
    }
}
