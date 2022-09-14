package com.reeva.regexp.compiler

import java.util.LinkedList

class Executor(private val start: Node, private val end: Node) {
    private val history = LinkedList<History>()

    fun execute(text: String): Boolean {
        val state = RegExpState(text)

        var activeNode = start
        var activeTransitionIndex = 0

        while (activeNode != end) {
            val transition = activeNode.transitions.getOrNull(activeTransitionIndex)

            if (transition == null) {
                // This node is exhausted, so backtrack

                if (history.isEmpty()) {
                    // Nothing to backtrack to
                    return false
                }

                val history = this.history.popLast()
                history.node.transitions[history.transitionIndex].transitionBackward(state)
                activeNode = history.node
                activeTransitionIndex = history.transitionIndex + 1

                continue
            }

            if (transition.first.test(state)) {
                transition.first.transitionForward(state)
            } else {
                activeTransitionIndex++
                continue
            }

            history.add(History(activeNode, activeTransitionIndex))
            activeNode = transition.second
            activeTransitionIndex = 0
        }

        return true
    }

    private data class History(val node: Node, var transitionIndex: Int)
}
