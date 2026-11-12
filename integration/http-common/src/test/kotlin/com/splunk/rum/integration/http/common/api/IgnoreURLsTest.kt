/*
 * Copyright 2026 Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum.integration.http.common.api

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.PatternSyntaxException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IgnoreURLsTest {
    @Test
    fun emptyRulesExcludeNothing() {
        val rules = IgnoreURLs()
        assertTrue(rules.isEmpty())
        assertFalse(rules.matches("https://example.com/private"))
    }

    @Test
    fun anyPatternCanMatchAnywhereInTheCompleteUrl() {
        val rules = IgnoreURLs(setOf("/private", "secret=", "never-matches"))
        assertTrue(rules.matches("https://example.com/private/account"))
        assertTrue(rules.matches("https://example.com/public?secret=value"))
        assertFalse(rules.matches("https://example.com/public"))
    }

    @Test
    fun anchorsCaseAndInlineFlagsAreRespected() {
        val rules = IgnoreURLs(setOf("^https://example\\.com/private$"))
        assertTrue(rules.matches("https://example.com/private"))
        assertFalse(rules.matches("https://example.com/private/child"))
        assertFalse(rules.matches("https://example.com/PRIVATE"))
        assertTrue(IgnoreURLs(setOf("(?i)/private")).matches("https://example.com/PRIVATE"))
    }

    @Test
    fun matchingDoesNotDecodeOrTruncateTheInput() {
        val rules = IgnoreURLs(setOf("%2Fprivate", "café"))
        assertTrue(rules.matches("https://example.com/?next=%2Fprivate"))
        assertFalse(rules.matches("https://example.com/?next=/private"))
        assertTrue(rules.matches("https://example.com/café"))
    }

    @Test(expected = PatternSyntaxException::class)
    fun invalidPatternRejectsTheRuleSetDuringConstruction() {
        IgnoreURLs(linkedSetOf("/valid", "["))
    }

    @Test
    fun anEmptyExpressionMatchesEveryUrl() {
        assertTrue(IgnoreURLs(setOf("")).matches("https://example.com/"))
    }

    @Test
    fun callerMutationCannotChangeTheRules() {
        val input = mutableSetOf("/private")
        val rules = IgnoreURLs(input)
        input.clear()
        input.add("/public")
        assertTrue(rules.matches("https://example.com/private"))
        assertFalse(rules.matches("https://example.com/public"))
    }

    @Test
    fun equalityUsesPatternValues() {
        val first = IgnoreURLs(linkedSetOf("/secret", "/private"))
        val second = IgnoreURLs(linkedSetOf("/private", "/secret"))
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun concurrentRequestsHaveIndependentMatchingState() {
        val rules = IgnoreURLs(setOf("/private$"))
        val executor = Executors.newFixedThreadPool(2)
        try {
            val futures = (0 until 5).map { index ->
                executor.submit<Boolean> {
                    val path = if (index % 2 == 0) "private" else "public"
                    rules.matches("https://example.com/$path") == (index % 2 == 0)
                }
            }
            futures.forEach { assertTrue(it.get(5, TimeUnit.SECONDS)) }
        } finally {
            executor.shutdownNow()
        }
    }
}
