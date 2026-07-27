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

package com.splunk.rum.instrumentation.crash

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import java.util.Collections
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CrashReporterTest {

    private val exportedLogs: MutableList<LogRecordData> = Collections.synchronizedList(mutableListOf())

    private val collectingExporter = object : LogRecordExporter {
        override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
            exportedLogs.addAll(logs)
            return CompletableResultCode.ofSuccess()
        }

        override fun flush() = CompletableResultCode.ofSuccess()
        override fun shutdown() = CompletableResultCode.ofSuccess()
    }

    private lateinit var sdk: OpenTelemetrySdk
    private var originalHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setUp() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        exportedLogs.clear()

        val loggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(collectingExporter))
            .build()

        sdk = OpenTelemetrySdk.builder()
            .setLoggerProvider(loggerProvider)
            .build()
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        sdk.shutdown()
    }

    @Test
    fun `emits crash event with expected scope, event name and exception details`() {
        val reporter = CrashReporter(sdk, emptyList())
        val thread = Thread.currentThread()
        val throwable = IllegalStateException("boom")

        reporter.report(CrashDetails(thread, throwable))

        val log = exportedLogs.single()
        assertEquals(CrashReporter.CRASH_INSTRUMENTATION_SCOPE_NAME, log.instrumentationScopeInfo.name)
        assertEquals(CrashReporter.CRASH_EVENT_NAME, log.attributes.get(EVENT_NAME_KEY))
        assertEquals(thread.id, log.attributes.get(THREAD_ID_KEY))
        assertEquals(thread.name, log.attributes.get(THREAD_NAME_KEY))
        assertEquals(IllegalStateException::class.java.name, log.attributes.get(EXCEPTION_TYPE_KEY))
        assertEquals("boom", log.attributes.get(EXCEPTION_MESSAGE_KEY))

        val stacktrace = log.attributes.get(EXCEPTION_STACKTRACE_KEY)
        assertNotNull(stacktrace)
        assertTrue(stacktrace!!.contains(IllegalStateException::class.java.name))
        assertTrue(stacktrace.contains("boom"))
    }

    @Test
    fun `emits crash event even when the exception has no message`() {
        val reporter = CrashReporter(sdk, emptyList())

        reporter.report(CrashDetails(Thread.currentThread(), RuntimeException()))

        val log = exportedLogs.single()
        assertEquals(RuntimeException::class.java.name, log.attributes.get(EXCEPTION_TYPE_KEY))
    }

    @Test
    fun `applies attributes from every extractor`() {
        val reporter = CrashReporter(
            sdk,
            listOf(
                CrashAttributesExtractor { attributes, _ -> attributes.put(AttributeKey.stringKey("first"), "1") },
                CrashAttributesExtractor { attributes, _ -> attributes.put(AttributeKey.stringKey("second"), "2") }
            )
        )

        reporter.report(CrashDetails(Thread.currentThread(), RuntimeException("boom")))

        val log = exportedLogs.single()
        assertEquals("1", log.attributes.get(AttributeKey.stringKey("first")))
        assertEquals("2", log.attributes.get(AttributeKey.stringKey("second")))
    }

    @Test
    fun `exposes crash details to extractors`() {
        val thread = Thread.currentThread()
        val throwable = RuntimeException("boom")
        var captured: CrashDetails? = null

        val reporter = CrashReporter(
            sdk,
            listOf(CrashAttributesExtractor { _, details -> captured = details })
        )

        reporter.report(CrashDetails(thread, throwable))

        assertEquals(CrashDetails(thread, throwable), captured)
    }

    @Test
    fun `a failing extractor does not prevent the crash from being reported`() {
        val reporter = CrashReporter(
            sdk,
            listOf(
                CrashAttributesExtractor { _, _ -> throw IllegalArgumentException("extractor failed") },
                CrashAttributesExtractor { attributes, _ -> attributes.put(AttributeKey.stringKey("survivor"), "yes") }
            )
        )

        reporter.report(CrashDetails(Thread.currentThread(), RuntimeException("boom")))

        val log = exportedLogs.single()
        assertEquals("yes", log.attributes.get(AttributeKey.stringKey("survivor")))
        assertEquals(CrashReporter.CRASH_EVENT_NAME, log.attributes.get(EVENT_NAME_KEY))
    }

    @Test
    fun `a custom extractor cannot override the crash event name`() {
        val reporter = CrashReporter(
            sdk,
            listOf(
                CrashAttributesExtractor { attributes, _ ->
                    attributes.put(AttributeKey.stringKey("event.name"), "custom.event")
                }
            )
        )

        reporter.report(CrashDetails(Thread.currentThread(), RuntimeException("boom")))

        val log = exportedLogs.single()
        assertEquals(CrashReporter.CRASH_EVENT_NAME, log.attributes.get(EVENT_NAME_KEY))
    }

    @Test
    fun `install registers a handler that reports the crash and chains the previous handler`() {
        var delegatedThread: Thread? = null
        var delegatedThrowable: Throwable? = null
        val previous = Thread.UncaughtExceptionHandler { t, e ->
            delegatedThread = t
            delegatedThrowable = e
        }
        Thread.setDefaultUncaughtExceptionHandler(previous)

        CrashReporter(sdk, emptyList()).install()

        val installed = Thread.getDefaultUncaughtExceptionHandler()
        assertTrue(installed is CrashReportingExceptionHandler)

        val thread = Thread.currentThread()
        val throwable = RuntimeException("boom")
        installed!!.uncaughtException(thread, throwable)

        assertEquals(CrashReporter.CRASH_EVENT_NAME, exportedLogs.single().attributes.get(EVENT_NAME_KEY))
        assertSame(thread, delegatedThread)
        assertSame(throwable, delegatedThrowable)
    }

    private companion object {
        val EVENT_NAME_KEY = AttributeKey.stringKey("event.name")
        val THREAD_ID_KEY = AttributeKey.longKey("thread.id")
        val THREAD_NAME_KEY = AttributeKey.stringKey("thread.name")
        val EXCEPTION_TYPE_KEY = AttributeKey.stringKey("exception.type")
        val EXCEPTION_MESSAGE_KEY = AttributeKey.stringKey("exception.message")
        val EXCEPTION_STACKTRACE_KEY = AttributeKey.stringKey("exception.stacktrace")
    }
}
