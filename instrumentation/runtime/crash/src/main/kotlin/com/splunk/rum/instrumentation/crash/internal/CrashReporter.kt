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

package com.splunk.rum.instrumentation.crash.internal

import com.splunk.rum.common.logger.Logger
import com.splunk.rum.instrumentation.crash.internal.extractor.CrashAttributesExtractor
import com.splunk.rum.instrumentation.crash.internal.extractor.CrashDetails
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.semconv.ExceptionAttributes
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Emits crash telemetry through OpenTelemetry and installs the uncaught exception handler.
 */
internal class CrashReporter(
    private val openTelemetry: OpenTelemetry,
    private val additionalExtractors: List<CrashAttributesExtractor>
) {

    /** Installs the crash reporting uncaught exception handler. */
    fun install() {
        val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
        val sdkLoggerProvider = (openTelemetry as? OpenTelemetrySdk)?.sdkLoggerProvider
        Thread.setDefaultUncaughtExceptionHandler(
            CrashReportingExceptionHandler(::report, sdkLoggerProvider, existingHandler)
        )
    }

    /** Emits a crash event for [crashDetails]. Never throws. */
    fun report(crashDetails: CrashDetails) {
        try {
            emitCrashEvent(crashDetails)
        } catch (e: Throwable) {
            Logger.e(TAG, "Failed to report crash", e)
        }
    }

    private fun emitCrashEvent(crashDetails: CrashDetails) {
        val thread = crashDetails.thread
        val cause = crashDetails.cause

        val attributes = Attributes.builder()
            .put(ThreadIncubatingAttributes.THREAD_ID, thread.id)
            .put(ThreadIncubatingAttributes.THREAD_NAME, thread.name)
            .put(ExceptionAttributes.EXCEPTION_TYPE, cause.javaClass.name)
            .put(ExceptionAttributes.EXCEPTION_STACKTRACE, stackTraceToString(cause))

        cause.message?.let { attributes.put(ExceptionAttributes.EXCEPTION_MESSAGE, it) }

        for (extractor in additionalExtractors) {
            try {
                extractor.extract(attributes, crashDetails)
            } catch (e: Throwable) {
                Logger.e(TAG, "Crash attributes extractor failed: ${extractor.javaClass.name}", e)
            }
        }

        // Set last so a custom extractor cannot override device.crash.
        attributes.put(LOG_EVENT_NAME_KEY, CRASH_EVENT_NAME)

        openTelemetry.logsBridge
            .get(CRASH_INSTRUMENTATION_SCOPE_NAME)
            .logRecordBuilder()
            .setAllAttributes(attributes.build())
            .emit()
    }

    private fun stackTraceToString(throwable: Throwable): String {
        val stringWriter = StringWriter(STACK_TRACE_INITIAL_SIZE)
        PrintWriter(stringWriter).use { throwable.printStackTrace(it) }
        return stringWriter.toString()
    }

    companion object {
        private const val TAG = "CrashReporter"
        private const val STACK_TRACE_INITIAL_SIZE = 256

        const val CRASH_INSTRUMENTATION_SCOPE_NAME = "com.splunk.rum.crash"
        const val CRASH_EVENT_NAME = "device.crash"

        private val LOG_EVENT_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("event.name")
    }
}
