/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.sdk.utils

import java.util.regex.Pattern

/**
 * Represents the parsed trace ID and span ID from the server-timing header.
 *
 * @property traceId The Trace ID extracted from the header.
 * @property spanId The Span ID extracted from the header.
 */
data class ServerTraceContext(val traceId: String, val spanId: String)

object ServerTimingHeaderParser {

    private val HEADER_PATTERN: Pattern = Pattern.compile(
        """traceparent;desc=['"]00-([0-9a-f]{32})-([0-9a-f]{16})-01['"]"""
    )

    /**
     * Parses the server-timing header to extract the trace ID and span ID.
     *
     * @param header A header string in the form:
     *     traceparent;desc="00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01"
     * @return A ServerTraceContext object containing TraceId and SpanId, or null if parsing fails.
     *      <p>This will also consider single-quotes valid for delimiting the "desc" section, even
     *      though it's not to spec.
     */
    fun parse(header: String?): ServerTraceContext? {
        if (header.isNullOrBlank()) {
            return null
        }

        val matcher = HEADER_PATTERN.matcher(header)
        if (matcher.matches()) {
            val traceId = matcher.group(1)
            val spanId = matcher.group(2)
            return ServerTraceContext(traceId, spanId)
        }

        return null
    }
}
