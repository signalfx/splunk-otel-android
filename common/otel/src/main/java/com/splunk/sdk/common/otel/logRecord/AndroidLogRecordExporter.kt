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

import com.splunk.sdk.common.otel.SplunkRumOpenTelemetrySdk
import com.splunk.sdk.common.otel.internal.RumConstants
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData
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

            // traceId and spanId should be inside the context already from global OTel instance
            val spanBuilder = SplunkRumOpenTelemetrySdk.instance!!.sdkTracerProvider.get(RumConstants.RUM_TRACER_NAME)
                .spanBuilder(log.attributes[AttributeKey.stringKey("event.name")] ?: "")
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(parentContext)
                .setStartTimestamp(log.timestampEpochNanos, TimeUnit.NANOSECONDS)


            if(activeSpan !=null && activeSpan.spanContext.isValid){
                spanBuilder.setParent(parentContext)
            }

            val span = spanBuilder.startSpan()

            try {
                span.setAttribute("log.body", log.body.asString())
                log.attributes.asMap().forEach { (key, value) ->
                    when (value) {
                        is String -> span.setAttribute(key.key, value)
                        is Long -> span.setAttribute(key.key, value)
                        is Double -> span.setAttribute(key.key, value)
                        is Boolean -> span.setAttribute(key.key, value)
                        is List<*> -> {
                            val listValue = value.joinToString(",") { it.toString() }
                            span.setAttribute(key.key, listValue)
                        }
                        else -> span.setAttribute(key.key, value.toString())
                    }
                }
            } finally {
                span.end(log.timestampEpochNanos, TimeUnit.NANOSECONDS)
            }
        }

        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }
}
