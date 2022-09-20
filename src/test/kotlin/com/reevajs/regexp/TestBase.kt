package com.reevajs.regexp

import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSuccess
import java.util.TreeMap

abstract class TestBase {
    protected fun testMatches(
        @Language("regexp") regexp: String, 
        value: String, 
        vararg flags: RegExp.Flag,
        block: MatchesBuilder.() -> Unit,
    ) {
        val builder = MatchesBuilder().apply(block)
        val matches = RegExp(regexp, *flags).match(value)

        if (builder.matches.isEmpty()) {
            expect(matches == null) {
                "Found no matches, but expected: $matches"
            }
        } else {
            expect(matches != null) {
                "Expected no matches, but found: $matches"
            }

            expectThat(matches.size)
                .describedAs("the number of matches is correct")
                .isEqualTo(builder.matches.size)

            builder.matches.zip(matches).forEach { (expected, actual) ->
                expectThat(expected).isEqualTo(actual)
            }
        }
    }

    protected fun testSyntaxError(@Language("regexp") regexp: String, vararg flags: RegExp.Flag) {
        expectThrows<RegexSyntaxError> { RegExp(regexp, *flags) }
    }

    protected fun testNoSyntaxError(@Language("regexp") regexp: String, vararg flags: RegExp.Flag) {
        expectCatching { RegExp(regexp, *flags) }.isSuccess()
    }

    class MatchesBuilder {
        val matches = mutableListOf<MatchResult>()

        fun match(block: MatchBuilder.() -> Unit) {
            val builder = MatchBuilder().apply(block)
            matches.add(MatchResult(builder.groups, builder.groupNames))
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
}
