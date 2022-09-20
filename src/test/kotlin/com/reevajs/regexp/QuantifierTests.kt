package com.reevajs.regexp

import org.junit.jupiter.api.Test

class QuantifierTests : TestBase() {
    @Test
    fun `test quantifier that consumes all input`() = testMatches("\\S+", "abcdefg") {
        match { this[0] = "abcdefg" spanned 0..6 }
    }

    @Test
    fun `test invalid quantifiers`() {
        val targets = listOf("a*", "a+", "a?", "a{1,2}")
        val quantifiers = listOf("*", "+", "{1,2}")

        for (target in targets) {
            for (quantifier in quantifiers)
                testSyntaxError(target + quantifier)
        }
    }
}
