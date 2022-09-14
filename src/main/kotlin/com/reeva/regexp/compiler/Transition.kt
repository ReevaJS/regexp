package com.reeva.regexp.compiler

import com.reeva.regexp.RegExpState

sealed interface Transition {
    fun test(state: RegExpState): Boolean

    fun transitionForward(state: RegExpState)

    fun transitionBackward(state: RegExpState)
}

object EpsilonTransition : Transition {
    override fun test(state: RegExpState) = true

    override fun transitionForward(state: RegExpState) {}
    override fun transitionBackward(state: RegExpState) {}
}

object StartTransition : Transition {
    override fun test(state: RegExpState) = state.cursor == 0

    override fun transitionForward(state: RegExpState) {}
    override fun transitionBackward(state: RegExpState) {}
}

object EndTransition : Transition {
    override fun test(state: RegExpState) = state.cursor == state.source.length

    override fun transitionForward(state: RegExpState) {}
    override fun transitionBackward(state: RegExpState) {}
}

class CharTransition(private val char: Char) : Transition {
    override fun test(state: RegExpState) = state.char == char

    override fun transitionForward(state: RegExpState) {
        state.cursor++
    }

    override fun transitionBackward(state: RegExpState) {
        state.cursor--
    }
}

class StringTransition(private val string: String) : Transition {
    override fun test(state: RegExpState) = state.source.substring(state.cursor).startsWith(string)

    override fun transitionForward(state: RegExpState) {
        state.cursor += string.length
    }

    override fun transitionBackward(state: RegExpState) {
        state.cursor -= string.length
    }
}
