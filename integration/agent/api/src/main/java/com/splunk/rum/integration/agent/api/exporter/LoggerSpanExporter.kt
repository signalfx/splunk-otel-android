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

package com.splunk.rum.integration.agent.api.exporter

import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.rum.common.otel.extensions.joinToString
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.atomic.AtomicBoolean

internal class LoggerSpanExporter : SpanExporter {

    private val isShutdown = AtomicBoolean(false)

    override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
        if (isShutdown.get()) {
            return CompletableResultCode.ofFailure()
        }

        spans.forEachFast { span ->
            val instrumentationScopeInfo = span.instrumentationScopeInfo

            Logger.i(TAG) {
                buildString {
                    append("name=${span.name}, ")
                    append("traceId=${span.traceId}, ")
                    append("spanId=${span.spanId}, ")
                    append("parentSpanId=${span.parentSpanId}, ")
                    append("kind=${span.kind}, ")
                    append("startEpochNanos=${span.startEpochNanos}, ")
                    append("endEpochNanos=${span.endEpochNanos}, ")
                    append("durationNanos=${span.endEpochNanos - span.startEpochNanos}, ")
                    append("status.code=${span.status.statusCode}, ")
                    append("status.description=${span.status.description}, ")
                    append("resources=${span.resource.attributes.joinToString(", ", "[", "]")}, ")
                    append("attributes=${span.attributes.joinToString(", ", "[", "]")}, ")
                    append("totalAttributeCount=${span.totalAttributeCount}, ")
                    append("events=${formatEvents(span.events)}, ")
                    append("totalRecordedEvents=${span.totalRecordedEvents}, ")
                    append("links=${formatLinks(span.links)}, ")
                    append("totalRecordedLinks=${span.totalRecordedLinks}, ")
                    append("instrumentationScopeInfo.name=${instrumentationScopeInfo.name}, ")
                    append("instrumentationScopeInfo.version=${instrumentationScopeInfo.version}")
                }
            }
        }

        return CompletableResultCode.ofSuccess()
    }

    private fun formatEvents(events: List<EventData>): String = buildString {
        append("[")
        events.forEachIndexed { index, event ->
            append("{name=${event.name}")
            append(", epochNanos=${event.epochNanos}")
            append(", attributes=${event.attributes.joinToString(", ", "[", "]")}")
            append("}")
            if (index < events.size - 1) append(", ")
        }
        append("]")
    }

    private fun formatLinks(links: List<LinkData>): String = buildString {
        append("[")
        links.forEachIndexed { index, link ->
            append("{traceId=${link.spanContext.traceId}")
            append(", spanId=${link.spanContext.spanId}")
            append(", attributes=${link.attributes.joinToString(", ", "[", "]")}")
            append("}")
            if (index < links.size - 1) append(", ")
        }
        append("]")
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = if (!isShutdown.compareAndSet(false, true)) {
        CompletableResultCode.ofSuccess()
    } else {
        flush()
    }

    private companion object {
        const val TAG = "LoggerSpanExporter"
    }
}
