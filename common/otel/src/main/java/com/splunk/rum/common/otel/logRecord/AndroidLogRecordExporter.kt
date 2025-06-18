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

package com.splunk.rum.common.otel.logRecord

import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.extensions.createZeroLengthSpan
import com.splunk.rum.common.otel.internal.RumConstants
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.data.internal.ExtendedLogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import java.util.concurrent.TimeUnit

/**
 * This Exporter is added to Otel by default, it handles the offline/persistance.
 */
internal class AndroidLogRecordExporter : LogRecordExporter {

    override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
        logs.forEach { log ->
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
