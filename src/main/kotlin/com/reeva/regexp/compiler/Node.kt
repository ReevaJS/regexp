package com.reeva.regexp.compiler

import com.reeva.regexp.RegExpState

class Node {
    private val transitionsBacker = mutableListOf<Pair<Transition, Node>>()

    val transitions: List<Pair<Transition, Node>>
        get() = transitionsBacker

    fun addTransition(transition: Transition, node: Node) {
        transitionsBacker.add(transition to node)
    }
}
