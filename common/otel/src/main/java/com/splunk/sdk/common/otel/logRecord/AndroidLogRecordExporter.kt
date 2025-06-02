/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.sdk.common.otel.logRecord

import com.cisco.android.common.job.IJobManager
import com.cisco.android.common.job.JobIdStorage
import com.splunk.sdk.common.otel.SplunkOpenTelemetrySdk
import com.splunk.sdk.common.otel.extensions.createZeroLengthSpan
import com.splunk.sdk.common.otel.internal.RumConstants
import com.splunk.sdk.common.otel.span.UploadSessionReplayData
import com.splunk.sdk.common.storage.IAgentStorage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.log

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

            // traceId and spanId should be inside the context already from global OTel instance
            val spanBuilder = SplunkOpenTelemetrySdk.instance!!.sdkTracerProvider.get(RumConstants.RUM_TRACER_NAME)
                .spanBuilder(log.attributes[AttributeKey.stringKey("event.name")] ?: "")
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
                spanBuilder.createZeroLengthSpan(log.timestampEpochNanos, TimeUnit.NANOSECONDS)

                if (log.instrumentationScopeInfo.name == RumConstants.CRASH_INSTRUMENTATION_SCOPE_NAME) {
                    SplunkOpenTelemetrySdk.instance?.sdkTracerProvider?.forceFlush()
                        ?.join(5, TimeUnit.SECONDS)
                }
            }
        }

        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
}
