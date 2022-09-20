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
}
