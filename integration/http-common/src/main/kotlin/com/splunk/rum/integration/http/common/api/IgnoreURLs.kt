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
import java.util.regex.PatternSyntaxException

/**
 * Immutable regular expressions identifying URLs to exclude from HTTP instrumentation.
 *
 * Patterns are compiled during construction. A URL is ignored when any pattern finds a match
 * anywhere in its complete, encoded URL string. Matching is case sensitive unless a pattern uses
 * an inline flag such as `(?i)`. An empty set excludes nothing; an empty expression matches every URL.
 * Prefer simple patterns without nested repetition to avoid expensive matching on request threads.
 *
 * The supplied set is copied, so subsequent changes to it cannot affect installed instrumentation.
 * Instances can be shared safely between module configurations.
 *
 * @param patterns Regular expression strings to compile using Android's [Pattern] implementation.
 * @throws PatternSyntaxException If any pattern is invalid. No partially valid rule set is created.
 */
class IgnoreURLs @JvmOverloads constructor(patterns: Set<String> = emptySet()) {
    private val patternStrings = patterns.toSet()
    private val compiledPatterns = patternStrings.map(Pattern::compile)

    /** Returns whether any expression finds a match in [url]. */
    fun matches(url: String): Boolean = compiledPatterns.any { it.matcher(url).find() }

    /** Returns whether this rule set contains no patterns. */
    fun isEmpty(): Boolean = compiledPatterns.isEmpty()

    override fun equals(other: Any?): Boolean = other is IgnoreURLs && patternStrings == other.patternStrings

    override fun hashCode(): Int = patternStrings.hashCode()
}
