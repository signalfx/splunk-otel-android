package com.splunk.sdk.utils

import java.util.regex.Pattern

object ServerTimingHeaderParser {

    private val UNPARSEABLE_RESULT = emptyArray<String>()
    private val HEADER_PATTERN: Pattern = Pattern.compile(
        """traceparent;desc=['"]00-([0-9a-f]{32})-([0-9a-f]{16})-01['"]"""
    )

    /**
     * Parses the server-timing header to extract the trace ID and span ID.
     *
     * @param header A header string in the form:
     *     traceparent;desc="00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01"
     * @return A two-element array containing TraceId and SpanId, or an empty array if parsing fails.
     *      <p>This will also consider single-quotes valid for delimiting the "desc" section, even
     *      though it's not to spec.
     */
    fun parse(header: String?): Array<String> {
        if (header.isNullOrBlank()) {
            return UNPARSEABLE_RESULT
        }

        val matcher = HEADER_PATTERN.matcher(header)
        if (matcher.matches()) {
            val traceId = matcher.group(1)
            val spanId = matcher.group(2)
            return arrayOf(traceId, spanId)
        }

        return UNPARSEABLE_RESULT
    }
}
