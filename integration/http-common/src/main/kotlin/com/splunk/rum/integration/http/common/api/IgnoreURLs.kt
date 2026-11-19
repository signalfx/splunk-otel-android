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

import java.util.regex.Pattern

/**
 * Immutable regular expressions identifying URLs to exclude from HTTP instrumentation.
 *
 * Compile patterns before construction using [Pattern.compile]. A URL is ignored when any pattern
 * finds a match anywhere in its complete, encoded URL string. An empty collection excludes
 * nothing; an empty expression matches every URL.
 * Prefer simple patterns without nested repetition to avoid expensive matching on request threads.
 *
 * The supplied patterns are copied, so subsequent changes to the input collection cannot affect installed instrumentation.
 * Instances can be shared safely between module configurations.
 *
 * @param patterns Precompiled regular expressions for URLs to exclude.
 */
class IgnoreURLs @JvmOverloads constructor(patterns: Collection<Pattern> = emptyList()) {
    // Match with one Pattern per distinct regex and flag combination, copied from the caller's collection.
    private val compiledPatterns = patterns.distinctBy { it.pattern() to it.flags() }

    // Compare rule sets by regex and flags, independent of Pattern identity or input order.
    private val patternValues = compiledPatterns.map { it.pattern() to it.flags() }.toSet()

    /** Returns whether any expression finds a match in [url]. */
    fun matches(url: String): Boolean = compiledPatterns.any { it.matcher(url).find() }

    /** Returns whether this rule set contains no patterns. */
    fun isEmpty(): Boolean = compiledPatterns.isEmpty()

    override fun equals(other: Any?): Boolean = other is IgnoreURLs && patternValues == other.patternValues

    override fun hashCode(): Int = patternValues.hashCode()
}
