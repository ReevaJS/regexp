package com.reevajs.regexp

import org.junit.jupiter.api.Test

class GroupTests : TestBase() {
    @Test
    fun `test empty capturing group`() = testMatches("(?:)", "abc") {
        match { this[0] = "" spanned IntRange(0, -1) }
        match { this[0] = "" spanned IntRange(1, 0) }
        match { this[0] = "" spanned IntRange(2, 1) }
        match { this[0] = "" spanned IntRange(3, 2) }
    }

    @Test
    fun `test missing capturing group`() = testMatches("(a)|(b)", "b") {
        match {
            this[0] = "b" spanned 0..0
            this[2] = "b" spanned 0..0
        }
    }

    @Test
    fun `test basic named capturing groups`() = testMatches("(foo)(?<b>bar)+", "foobarbar") {
        match {
            this[0] = "foobarbar" spanned 0..8
            this[1] = "foo" spanned 0..2
            this[2] = "bar" spanned 6..8 named "b"
        }
    }

    @Test
    fun `test forward capturing group captures nothing`() = testMatches("\\k<a>(?<a>x)", "x") {
        match {
            this[0] = "x" spanned 0..0
            this[1] = "x" spanned 0..0 named "a"
        }
    }
}
