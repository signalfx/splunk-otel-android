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

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportingExceptionHandlerTest {

    @Test
    fun `reports crash then delegates to existing handler with same arguments`() {
        val callOrder = mutableListOf<String>()
        var reportedDetails: CrashDetails? = null
        var delegatedThread: Thread? = null
        var delegatedThrowable: Throwable? = null

        val existingHandler = Thread.UncaughtExceptionHandler { t, e ->
            callOrder += "existing"
            delegatedThread = t
            delegatedThrowable = e
        }

        val handler = CrashReportingExceptionHandler(
            crashSender = { details ->
                callOrder += "report"
                reportedDetails = details
            },
            sdkLoggerProvider = null,
            existingHandler = existingHandler
        )

        val thread = Thread.currentThread()
        val throwable = RuntimeException("boom")

        handler.uncaughtException(thread, throwable)

        assertEquals(listOf("report", "existing"), callOrder)
        assertEquals(CrashDetails(thread, throwable), reportedDetails)
        assertSame(thread, delegatedThread)
        assertSame(throwable, delegatedThrowable)
    }

    @Test
    fun `reports crash when there is no existing handler`() {
        var reported = false

        val handler = CrashReportingExceptionHandler(
            crashSender = { reported = true },
            sdkLoggerProvider = null,
            existingHandler = null
        )

        handler.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

        assertTrue(reported)
    }

    @Test
    fun `flushes telemetry synchronously before delegating`() {
        val exportedLogs: MutableList<LogRecordData> = Collections.synchronizedList(mutableListOf())
        val exporter = object : LogRecordExporter {
            override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
                exportedLogs.addAll(logs)
                return CompletableResultCode.ofSuccess()
            }

            override fun flush() = CompletableResultCode.ofSuccess()
            override fun shutdown() = CompletableResultCode.ofSuccess()
        }

        // A batch processor does not export on emit, only on (force)flush - so a non-empty export
        // list after uncaughtException proves the handler flushed synchronously.
        val loggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(BatchLogRecordProcessor.builder(exporter).build())
            .build()
        val logger = loggerProvider.get("test")

        val callOrder = mutableListOf<String>()
        val handler = CrashReportingExceptionHandler(
            crashSender = {
                callOrder += "report"
                logger.logRecordBuilder().emit()
            },
            sdkLoggerProvider = loggerProvider,
            existingHandler = Thread.UncaughtExceptionHandler { _, _ -> callOrder += "existing" }
        )

        handler.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

        assertEquals(1, exportedLogs.size)
        assertEquals(listOf("report", "existing"), callOrder)

        loggerProvider.shutdown()
    }
}
