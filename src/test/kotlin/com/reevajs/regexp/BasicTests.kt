package com.reevajs.regexp

import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.util.TreeMap

object BasicTests {
    @Suppress("SpellCheckingInspection")
    @JvmStatic
    fun tests() = TestBuilder().apply {
        // Forward-capturing group
        testMatches("\\k<a>(?<a>x)", "x") {
            match {
                this[0] = "x" spanned 0..0
                this[1] = "x" spanned 0..0 named "a"
            }
        }

        // Basic named capturing group
        testMatches("(?<name>foo)(bar)+", "foobarbarbar") {
            match {
                this[0] = "foobarbarbar" spanned 0..11
                this[1] = "foo" spanned 0..2 named "name"
                this[2] = "bar" spanned 9..11
            }
        }

        // Missing indexed group
        testMatches("(a)|(b)", "b") {
            match {
                this[0] = "b" spanned 0..0
                this[2] = "b" spanned 0..0
            }
        }

        // Matching until end of input
        testMatches("\\S+", "abcdefg") {
            match {
                this[0] = "abcdefg" spanned 0..6
            }
        }

        // Negated entry in char class
        testMatches("[\\S]+", "abcdefg") {
            match {
                this[0] = "abcdefg" spanned 0..6
            }
        }

        // Complex class based test
        testMatches("""([\S]+([ \t]+[\S]+)*)[ \t]*=[ \t]*[\S]+""", "Test_Key = Value") {
            match {
                this[0] = "Test_Key = Value" spanned 0..15
                this[1] = "Test_Key" spanned 0..7
            }
        }

        // Empty match
        testMatches("(?:)", "abc") {
            match { this[0] = "" spanned IntRange(0, -1) }
            match { this[0] = "" spanned IntRange(1, 0) }
            match { this[0] = "" spanned IntRange(2, 1) }
            match { this[0] = "" spanned IntRange(3, 2) }
        }
    }.tests

    @ParameterizedTest
    @MethodSource("tests")
    fun runTests(test: Test) {
        val matches = RegExp(test.regex.codePoints().toArray(), test.flags).match(test.value)

        if (test.expectedResult == null) {
            expectThat(matches).isNull()
        } else {
            expect(matches != null) {
                "Found no matches, but expected: ${test.expectedResult}"
            }

            expectThat(matches.size).isEqualTo(test.expectedResult.size)

            test.expectedResult.zip(matches).forEach { (expected, actual) ->
                expectThat(expected).isEqualTo(actual)
            }
        }
    }

    class TestBuilder {
        val tests = mutableListOf<Test>()

        fun testNoMatch(@Language("regexp") regex: String, value: String) {
            tests.add(Test(regex, value, emptySet(), null))
        }

        fun testMatches(@Language("regexp") regex: String, value: String, vararg flags: RegExp.Flag, matchesBuilder: MatchesBuilder.() -> Unit) {
            val matches = MatchesBuilder().apply(matchesBuilder)
            val results = matches.matches.map {
                MatchResult(it.groups, it.groupNames)
            }
            tests.add(Test(regex, value, flags.toSet(), results))
        }
    }

    class MatchesBuilder {
        val matches = mutableListOf<MatchBuilder>()

        fun match(builder: MatchBuilder.() -> Unit) {
            matches.add(MatchBuilder().apply(builder))
        }
    }

    class MatchBuilder {
        val groups = TreeMap<Int, MatchGroup>()
        val groupNames = mutableMapOf<Int, String>()
        private var pendingName: String? = null

        operator fun set(index: Int, group: MatchGroup) {
            groups[index] = group
            if (pendingName != null) {
                groupNames[index] = pendingName!!
                pendingName = null
            }
        }

        infix fun String.spanned(range: IntRange) = MatchGroup(codePoints().toArray(), range)

        infix fun MatchGroup.named(name: String) = apply {
            pendingName = name
        }
    }

    sealed class ExpectedResult

    data class Test(
        val regex: String,
        val value: String,
        val flags: Set<RegExp.Flag>,
        val expectedResult: List<MatchResult>?,
    )
}
