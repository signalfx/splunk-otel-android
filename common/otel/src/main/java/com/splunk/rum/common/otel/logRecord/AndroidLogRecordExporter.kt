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

package com.splunk.rum.common.otel.logRecord

import com.cisco.android.common.job.IJobManager
import com.cisco.android.common.job.JobIdStorage
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.extensions.createZeroLengthSpan
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.common.otel.span.UploadSessionReplayData
import com.splunk.rum.common.storage.IAgentStorage
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.data.internal.ExtendedLogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * This Exporter is added to Otel by default, it handles the offline/persistance.
 */
internal class AndroidLogRecordExporter(
    private val agentStorage: IAgentStorage,
    private val jobManager: IJobManager,
    private val jobIdStorage: JobIdStorage
) : LogRecordExporter {

    override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
        val sessionReplayLogs =
            logs.filter { it.instrumentationScopeInfo.name == RumConstants.SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME }
        val generalLogs =
            logs.filter { it.instrumentationScopeInfo.name != RumConstants.SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME }

        // We need special handling of Session Replay data because of the current state of the backend implementation.
        if (sessionReplayLogs.isNotEmpty()) {
            val exportRequest = LogsRequestMarshaler.create(sessionReplayLogs)
            val id = UUID.randomUUID().toString()

            // Save data to our storage.
            ByteArrayOutputStream().use {
                exportRequest.writeBinaryTo(it)
                agentStorage.writeOtelSessionReplayData(id, it.toByteArray())
            }

            // Job scheduling
            jobManager.scheduleJob(UploadSessionReplayData(id, jobIdStorage))
        }

        generalLogs.forEach { log ->
            val parentContext = Context.current()
            val activeSpan = Span.fromContextOrNull(parentContext)

            /**
             * Determines the name of the span to be created from the log record.
             *
             * The resolution order is as follows:
             * 1. Use the `eventName` property from `ExtendedLogRecordData` if available.
             * 2. Otherwise, fall back to the [RumConstants.LOG_EVENT_NAME_KEY] attribute in the log's attributes.
             * 3. If neither is present, default to the name [RumConstants.DEFAULT_LOG_EVENT_NAME].
             *
             * This ensures that the span always has a meaningful or fallback name, even when
             * the source log record lacks explicit naming metadata.
             */
            val spanName = (log as ExtendedLogRecordData).eventName
                ?: log.attributes.get(RumConstants.LOG_EVENT_NAME_KEY)
                ?: RumConstants.DEFAULT_LOG_EVENT_NAME

            // traceId and spanId should be inside the context already from global OTel instance
            val spanBuilder = SplunkOpenTelemetrySdk.instance!!.sdkTracerProvider.get(RumConstants.RUM_TRACER_NAME)
                .spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(parentContext)
                .setStartTimestamp(log.timestampEpochNanos, TimeUnit.NANOSECONDS)

            if (activeSpan != null && activeSpan.spanContext.isValid) {
                spanBuilder.setParent(parentContext)
            }

            try {
                if (log.bodyValue != null) {
                    spanBuilder.setAttribute("body", log.bodyValue.toString())
                }

                log.attributes.asMap().forEach { (key, value) ->
                    when (value) {
                        is String -> spanBuilder.setAttribute(key.key, value)
                        is Long -> spanBuilder.setAttribute(key.key, value)
                        is Double -> spanBuilder.setAttribute(key.key, value)
                        is Boolean -> spanBuilder.setAttribute(key.key, value)
                        is List<*> -> {
                            val listValue = value.joinToString(",") { it.toString() }
                            spanBuilder.setAttribute(key.key, listValue)
                        }
                        else -> spanBuilder.setAttribute(key.key, value.toString())
                    }
                }
            } finally {
                val effectiveTimestamp = log.timestampEpochNanos.takeIf { it != 0L }
                    ?: log.observedTimestampEpochNanos

                if (log.instrumentationScopeInfo.name == RumConstants.CRASH_INSTRUMENTATION_SCOPE_NAME) {
                    val span = spanBuilder.setStartTimestamp(effectiveTimestamp, TimeUnit.NANOSECONDS).startSpan()
                    val spanData = (span as? io.opentelemetry.sdk.trace.ReadableSpan)?.toSpanData()
                    span.end(effectiveTimestamp, TimeUnit.NANOSECONDS)

                    if (spanData != null) {
                        val crashSpanId = UUID.randomUUID().toString()
                        val exportRequest = TraceRequestMarshaler.create(listOf(spanData))

                        ByteArrayOutputStream().use {
                            exportRequest.writeBinaryTo(it)
                            val success = agentStorage.writeOtelSpanData(crashSpanId, it.toByteArray())
                        }
                        agentStorage.addBufferedSpanId(crashSpanId)
                    }
                } else {
                    spanBuilder.createZeroLengthSpan(effectiveTimestamp, TimeUnit.NANOSECONDS)
                }
            }
        }

        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
}
