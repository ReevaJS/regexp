package com.reevajs.regexp

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class EscapeTests : TestBase() {
    @Test
    fun `test digit escape`() = escapeTest("\\d", '0'.code..'9'.code)
    
    @Test
    fun `test negated digit escape`() = escapeTest("\\D", 0x0..0x2f, 0x3a..0x10ffff)

    @Test
    fun `test whitespace escape`() = escapeTest(
        "\\s",
        0x0020, 0x00a0, 0x1680, 0x202f, 0x205f, 0x3000, 0xfeff,
        0x0009..0x000d, 0x2000..0x200a, 0x2028..0x2029,
    )

    @Test
    fun `test negated whitespace escape`() = escapeTest(
        "\\S",
        0x00dc00..0x00dfff,
        0x000000..0x000008,
        0x00000e..0x00001f,
        0x000021..0x00009f,
        0x0000a1..0x00167f,
        0x001681..0x001fff,
        0x00200b..0x002027,
        0x00202a..0x00202e,
        0x002030..0x00205e,
        0x002060..0x002fff,
        0x003001..0x00dbff,
        0x00e000..0x00fefe,
        0x00ff00..0x10ffff,
    )

    @Test
    fun `test word escape`() = escapeTest("\\w", 0x5f, 0x30..0x39, 0x41..0x5a, 0x61..0x7a)

    @Test
    fun `test neated word escape`() = escapeTest(
        "\\W",
        0x00dc00..0x00dfff,
        0x000000..0x00002f,
        0x00003a..0x000040,
        0x00005b..0x00005e,
        0x00007b..0x00dbff,
        0x00e000..0x10ffff,
    )

    private fun escapeTest(@Language("regexp") regexp: String, vararg codePoints: Any /* Int | IntRange */) {
        val cpRanges = codePoints.map {
            when (it) {
                is Int -> it..it
                is IntRange -> it
                else -> unreachable()
            }
        }

        for (range in cpRanges) {
            (range.first..range.last).forEach {
                testMatchesEntire(regexp, intArrayOf(it).codePointsToString())
            }
        }
    }
}
