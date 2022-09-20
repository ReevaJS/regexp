package com.reevajs.regexp

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

const val DEBUG = false

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
    cp in 'A'.code..'Z'.code || cp in 'a'.code..'z'.code || cp in '0'.code..'9'.code || cp == '_'.code
