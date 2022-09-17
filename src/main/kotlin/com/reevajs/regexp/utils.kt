package com.reevajs.regexp

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun unreachable(): Nothing {
    throw IllegalStateException("Encountered unreachable() call")
}

@OptIn(ExperimentalContracts::class)
fun expect(condition: Boolean, message: String? = null) {
    contract {
        returns() implies condition
    }

    if (!condition)
        throw ExpectationError(message ?: "Expectation failed")
}

@OptIn(ExperimentalContracts::class)
fun expect(condition: Boolean, messageProvider: () -> String) {
    contract {
        returns() implies condition
    }

    if (!condition)
        throw ExpectationError(messageProvider())
}

class ExpectationError(message: String) : Exception(message)

fun IntArray.codePointsToString() = buildString { forEach(::appendCodePoint) }

fun isWordCodepoint(cp: Int): Boolean =
    cp in 0x41..0x5a /*A-Z*/ || cp in 0x61..0x7a /*a-z*/ || cp in 0x30..0x39 /*0-9*/ || cp == 0x5f /*_*/

// TODO: Are these the only characters? Does unicode mode change this?
fun isWhitespaceCodepoint(cp: Int): Boolean =
    cp == 0x20 /* <space> */ || cp == 0x9 /* <tab> */ || cp == 0xa /* <new line> */ || cp == 0xd /* <carriage return> */
