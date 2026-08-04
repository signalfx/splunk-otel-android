/*
 * Copyright 2026 Splunk Inc.
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.anr.internal

import com.splunk.rum.common.logger.Logger
import com.splunk.rum.instrumentation.anr.internal.extractor.AnrAttributesExtractor
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.semconv.ExceptionAttributes

/**
 * Emits ANR telemetry through OpenTelemetry as an error span carrying the main thread's stack trace.
 */
internal class AnrReporter(
    private val openTelemetry: OpenTelemetry,
    private val additionalExtractors: List<AnrAttributesExtractor>
) {

    /** Emits an ANR event for the given main-thread [stackTrace]. Never throws. */
    fun report(stackTrace: Array<StackTraceElement>) {
        try {
            emitAnrEvent(stackTrace)
        } catch (e: Throwable) {
            Logger.e(TAG, "Failed to report ANR", e)
        }
    }

    private fun emitAnrEvent(stackTrace: Array<StackTraceElement>) {
        val attributes = Attributes.builder()
            .put(ExceptionAttributes.EXCEPTION_STACKTRACE, formatStackTrace(stackTrace))

        for (extractor in additionalExtractors) {
            try {
                extractor.extract(attributes, stackTrace)
            } catch (e: Throwable) {
                Logger.e(TAG, "ANR attributes extractor failed: ${extractor.javaClass.name}", e)
            }
        }

        val span = openTelemetry.getTracer(ANR_INSTRUMENTATION_SCOPE_NAME)
            .spanBuilder(ANR_SPAN_NAME)
            .setAllAttributes(attributes.build())
            .startSpan()

        try {
            span.setStatus(StatusCode.ERROR)
        } finally {
            span.end()
        }
    }

    private fun formatStackTrace(stackTrace: Array<StackTraceElement>): String {
        val builder = StringBuilder()
        for (element in stackTrace) {
            builder.append(element).append('\n')
        }
        return builder.toString()
    }

    companion object {
        private const val TAG = "AnrReporter"

        const val ANR_INSTRUMENTATION_SCOPE_NAME = "com.splunk.rum.anr"
        const val ANR_SPAN_NAME = "ANR"
    }
}
