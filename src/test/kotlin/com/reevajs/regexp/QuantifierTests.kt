package com.reevajs.regexp

import org.junit.jupiter.api.Test

class QuantifierTests : TestBase() {
    @Test
    fun `test quantifier that consumes all input`() = testMatches("\\S+", "abcdefg") {
        match { this[0] = "abcdefg" spanned 0..6 }
    }

    @Test
    fun `test basic repetition quantifier`() = testMatches("\\w{2,3}", "abcde") {
        match { this[0] = "abc" spanned 0..2 }
        match { this[0] = "de" spanned 3..4 }
    }

    @Test
    fun `test invalid quantifiers`() {
        val targets = listOf("a*", "a+", "a?", "a{1,2}")
        val quantifiers = listOf("*", "+", "{1,2}")

        for (target in targets) {
            for (quantifier in quantifiers)
                testDoesNotCompile(target + quantifier)
        }
    }

    @Test
    fun `test unbounded empty basic quantifier match does not infinitely loop`() = testMatches("(?:)*", "ab") {
        match { this[0] = "" spanned IntRange(0, -1) }
        match { this[0] = "" spanned IntRange(1, 0) }
        match { this[0] = "" spanned IntRange(2, 1) }
    }

    @Test
    fun `test unbounded empty repetition quantifier match does not infinitely loop`() = testMatches("(?:){0,}", "ab") {
        match { this[0] = "" spanned IntRange(0, -1) }
        match { this[0] = "" spanned IntRange(1, 0) }
        match { this[0] = "" spanned IntRange(2, 1) }
    }
}
