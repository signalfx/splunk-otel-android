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

package com.splunk.rum.integration.agent.api.exporter

import com.splunk.android.common.logger.Logger
import com.splunk.rum.common.otel.extensions.appendTo
import com.splunk.rum.common.otel.internal.RumConstants
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class LoggerLogRecordExporter : LogRecordExporter {

    private val isShutdown = AtomicBoolean(false)
    private val loggingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
        if (isShutdown.get()) {
            return CompletableResultCode.ofFailure()
        }

        // Execute logging asynchronously on background thread
        loggingScope.launch {
            logs.forEach { log ->
                val instrumentationScopeInfo = log.instrumentationScopeInfo

                /**
                 * FIXME
                 *
                 * This is a simple hack that ensures that only logs from the session replay instrumentation are
                 * processed by this exporter, because all other logs are currently transformed into spans
                 * and would therefore be logged twice.
                 *
                 * This should be reworked into a more general solution that properly resolves the logging duplication.
                 */
                if (instrumentationScopeInfo.name != RumConstants.SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME) {
                    return@forEach
                }

                val message = buildString {
                    append("severityText=").append(log.severityText)
                    append(", severity=").append(log.severity)
                    append(", timestampEpochNanos=").append(log.timestampEpochNanos)
                    append(", observedTimestampEpochNanos=").append(log.observedTimestampEpochNanos)
                    append(", traceId=").append(log.spanContext.traceId)
                    append(", spanId=").append(log.spanContext.spanId)
                    append(", traceFlags=").append(log.spanContext.traceFlags)
                    append(", resources=")
                    log.resource.attributes.appendTo(this)
                    append(", attributes=")
                    log.attributes.appendTo(this)
                    append(", totalAttributeCount=").append(log.totalAttributeCount)
                    append(", instrumentationScopeInfo.name=").append(instrumentationScopeInfo.name)
                    append(", instrumentationScopeInfo.version=").append(instrumentationScopeInfo.version)
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
        const val TAG = "LoggerLogRecordExporter"
    }
}
