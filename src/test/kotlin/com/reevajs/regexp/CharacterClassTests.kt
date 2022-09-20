package com.reevajs.regexp

import org.junit.jupiter.api.Test

class CharacterClassTests : TestBase() {
    @Test
    fun `test basic class`() = testMatches("[a-c]", "abcd") {
        match { this[0] = "a" spanned 0..0 }
        match { this[0] = "b" spanned 1..1 }
        match { this[0] = "c" spanned 2..2 }
    }

    @Test
    fun `test hex escapes in class`() = testMatches("[\\x61-\\x63]", "abcd") {
        match { this[0] = "a" spanned 0..0 }
        match { this[0] = "b" spanned 1..1 }
        match { this[0] = "c" spanned 2..2 }
    }

    @Test
    fun `test backref-like escapes behave like octal escapes in classes`() = testMatches("(a)[\\1]", "a\u0001") {
        match { 
            this[0] = "a\u0001" spanned 0..1 
            this[1] = "a" spanned 0..0
        }
    }

    @Test
    fun `test negated entry in class`() = testMatches("[\\S]+", "abcdefg") {
        match { this[0] = "abcdefg" spanned 0..6 }
    }

    @Test
    fun `test out of order range`() = testDoesNotCompile("[b-a]")

    @Test
    fun `test meta escapes in class ranges are a syntax error`() = listOf(
        "\\s", "\\S", "\\d", "\\D", "\\w", "\\W"
    ).forEach {
        testDoesNotCompile("[a-$it]")
    }
}
