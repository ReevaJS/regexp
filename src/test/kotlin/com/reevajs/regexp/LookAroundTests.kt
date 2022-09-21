package com.reevajs.regexp

import org.junit.jupiter.api.Test

class LookAroundTests : TestBase() {
    // https://github.com/tc39/test262/blob/main/test/built-ins/RegExp/lookBehind/mutual-recursive.js
    @Test
    fun `test lookbehind mutual recursion`() {
        testMatches("(?<=a(.\\2)b(\\1)).{4}", "aabcacbc") {
            match {
                this[0] = "cacb" spanned 3..6
                this[1] = "a" spanned 1..1
                this[2] = "" spanned 3..2
            }
        }

        testMatches("(?<=a(\\2)b(..\\1))b", "aacbacb") {
            match {
                this[0] = "b" spanned 6..6
                this[1] = "ac" spanned 1..2
                this[2] = "ac" spanned 4..5
            }
        }

        testMatches("(?<=(?:\\1b)(aa)).", "aabaax") {
            match {
                this[0] = "x" spanned 5..5
                this[1] = "aa" spanned 3..4
            }
        }

        testMatches("(?<=(?:\\1|b)(aa)).", "aaaax") {
            match {
                this[0] = "x" spanned 4..4
                this[1] = "aa" spanned 2..3
            }
        }
    }
}
