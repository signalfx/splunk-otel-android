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
import com.splunk.rum.common.otel.extensions.appendAttributes
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
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
            Logger.i(TAG, span.toLogMessage())
        }

        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = if (!isShutdown.compareAndSet(false, true)) {
        CompletableResultCode.ofSuccess()
    } else {
        flush()
    }

    private fun SpanData.toLogMessage(): String {
        val instrumentationScopeInfo = instrumentationScopeInfo

        return buildString {
            append("name=")
            append(name)
            append(", traceId=")
            append(traceId)
            append(", spanId=")
            append(spanId)
            append(", parentSpanId=")
            append(parentSpanId)
            append(", kind=")
            append(kind)
            append(", resources=")
            appendAttributes(resource.attributes)
            append(", attributes=")
            appendAttributes(attributes)
            append(", instrumentationScopeInfo.name=")
            append(instrumentationScopeInfo.name)
            append(", instrumentationScopeInfo.version=")
            append(instrumentationScopeInfo.version)
        }
    }

    private companion object {
        const val TAG = "LoggerSpanExporter"
    }
}
