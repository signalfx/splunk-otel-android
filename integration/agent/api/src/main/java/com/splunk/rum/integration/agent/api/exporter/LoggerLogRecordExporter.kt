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
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import java.util.concurrent.atomic.AtomicBoolean

internal class LoggerLogRecordExporter : LogRecordExporter {

    private val isShutdown = AtomicBoolean(false)

    override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
        if (isShutdown.get()) {
            return CompletableResultCode.ofFailure()
        }

        for (log in logs) {
            val instrumentationScopeInfo = log.instrumentationScopeInfo
            Logger.i(
                TAG,
                "bodyValue=${log.bodyValue}, " +
                        "severityText=${log.severityText}, " +
                        "severity=${log.severity}, " +
                        "timestampEpochNanos=${log.timestampEpochNanos}, " +
                        "observedTimestampEpochNanos=${log.observedTimestampEpochNanos}, " +
                        "traceId=${log.spanContext.traceId}, " +
                        "spanId=${log.spanContext.spanId}, " +
                        "traceFlags=${log.spanContext.traceFlags}, " +
                        "resources=${log.resource.attributes.toSplunkString()}, " +
                        "attributes=${log.attributes.toSplunkString()}, " +
                        "totalAttributeCount=${log.totalAttributeCount}, " +
                        "instrumentationScopeInfo.name=${instrumentationScopeInfo.name}, " +
                        "instrumentationScopeInfo.version=${instrumentationScopeInfo.version}"
            )
        }

        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = if (!isShutdown.compareAndSet(false, true)) {
        CompletableResultCode.ofSuccess()
    } else {
        flush()
    }

    private fun Attributes.toSplunkString(): String = asMap()
        .toList()
        .joinToString(", ", "[", "]") { "${it.first}=${it.second}" }

    private companion object {
        const val TAG = "LoggerLogRecordExporter"
    }
}