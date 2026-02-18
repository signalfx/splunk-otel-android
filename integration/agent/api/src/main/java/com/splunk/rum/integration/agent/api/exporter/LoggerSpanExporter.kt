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
import com.splunk.rum.common.otel.extensions.appendTo
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class LoggerSpanExporter : SpanExporter {

    private val isShutdown = AtomicBoolean(false)
    private val loggingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
        if (isShutdown.get()) {
            return CompletableResultCode.ofFailure()
        }

        // Execute logging asynchronously on background thread
        loggingScope.launch {
            spans.forEach { span ->
                val message = buildString {
                    append("name=").append(span.name)
                    append(", traceId=").append(span.traceId)
                    append(", spanId=").append(span.spanId)
                    append(", parentSpanId=").append(span.parentSpanId)
                    append(", kind=").append(span.kind)
                    append(", resources=")
                    span.resource.attributes.appendTo(this)
                    append(", attributes=")
                    span.attributes.appendTo(this)
                    append(", instrumentationScopeInfo.name=").append(span.instrumentationScopeInfo.name)
                    append(", instrumentationScopeInfo.version=").append(span.instrumentationScopeInfo.version)
                }

                Logger.i(TAG, message)
            }
        }

        // Return success immediately without blocking
        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = if (!isShutdown.compareAndSet(false, true)) {
        CompletableResultCode.ofSuccess()
    } else {
        loggingScope.cancel()
        flush()
    }

    private companion object {
        const val TAG = "LoggerSpanExporter"
    }
}
