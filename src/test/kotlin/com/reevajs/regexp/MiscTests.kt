package com.reevajs.regexp

import org.junit.jupiter.api.Test

class MiscTests : TestBase() {
    @Test
    fun `test a complex key-value regex`() = testMatches("""([\S]+([ \t]+[\S]+)*)[ \t]*=[ \t]*[\S]+""", "Test_Key = Value") {
        match {
            this[0] = "Test_Key = Value" spanned 0..15
            this[1] = "Test_Key" spanned 0..7
        }
    }

    @Test
    fun `test dot without newline flag`() {
        for (flags in listOf(setOf(RegExp.Flag.MultiLine), emptySet())) {
            testMatchesEntire("^.$", "a", *flags.toTypedArray())
            testMatchesEntire("^.$", "3", *flags.toTypedArray())
            testMatchesEntire("^.$", "π", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u2027", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u0085", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u000b", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u000c", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u180e", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u180e", *flags.toTypedArray())
            testNotMatches("^.$", "\ud800\udf00", *flags.toTypedArray()) // \u{10300}
            testNotMatches("^.$", "\n", *flags.toTypedArray())
            testNotMatches("^.$", "\r", *flags.toTypedArray())
            testNotMatches("^.$", "\u2028", *flags.toTypedArray())
            testNotMatches("^.$", "\u2029", *flags.toTypedArray())
            testMatchesEntire("^.$", "\ud800", *flags.toTypedArray())
            testMatchesEntire("^.$", "\udfff", *flags.toTypedArray())
        }

        testMatchesEntire("^.$", "\ud800\udf00", RegExp.Flag.MultiLine, RegExp.Flag.Unicode)
    }

    @Test
    fun `test dot with newline flag`() {
        for (flags in listOf(setOf(RegExp.Flag.DotMatchesNewlines, RegExp.Flag.MultiLine), setOf(RegExp.Flag.DotMatchesNewlines))) {
            testMatchesEntire("^.$", "a", *flags.toTypedArray())
            testMatchesEntire("^.$", "3", *flags.toTypedArray())
            testMatchesEntire("^.$", "π", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u2027", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u0085", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u000b", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u000c", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u180e", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u180e", *flags.toTypedArray())
            testNotMatches("^.$", "\ud800\udf00", *flags.toTypedArray()) // \u{10300}
            testMatchesEntire("^.$", "\n", *flags.toTypedArray())
            testMatchesEntire("^.$", "\r", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u2028", *flags.toTypedArray())
            testMatchesEntire("^.$", "\u2029", *flags.toTypedArray())
            testMatchesEntire("^.$", "\ud800", *flags.toTypedArray())
            testMatchesEntire("^.$", "\udfff", *flags.toTypedArray())
        }

        testMatchesEntire("^.$", "\ud800\udf00", RegExp.Flag.DotMatchesNewlines, RegExp.Flag.MultiLine, RegExp.Flag.Unicode)
    }
}
