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

package com.splunk.rum.integration.agent.api

import com.splunk.android.common.logger.Logger
import com.splunk.rum.common.storage.IAgentStorage
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import java.util.concurrent.atomic.AtomicReference

class AgentPreferences internal constructor(
    private val agentStorage: IAgentStorage?,
    private val endpointRef: AtomicReference<EndpointConfiguration?>,
    private val endpointLock: Any,
    private val openTelemetry: OpenTelemetry
) {

    var endpointConfiguration: EndpointConfiguration?
        get() = endpointRef.get()
        set(value) {
            value?.let { newEndpoint: EndpointConfiguration ->
                synchronized(endpointLock) {
                    val storage = agentStorage ?: run {
                        Logger.w(TAG, "Cannot set endpoint: storage not available")
                        return@synchronized
                    }

                    storage.writeTracesBaseUrl(newEndpoint.tracesEndpoint!!.toExternalForm())
                    newEndpoint.logsEndpoint?.let { logsUrl ->
                        storage.writeLogsBaseUrl(logsUrl.toExternalForm())
                    }

                    endpointRef.set(newEndpoint)

                    Logger.d(TAG, "Endpoint configured, flushing cached data")

                    (openTelemetry as? OpenTelemetrySdk)?.let { sdk ->
                        sdk.sdkTracerProvider?.forceFlush()
                        sdk.sdkLoggerProvider?.forceFlush()
                    }
                }
            }
        }

    companion object {
        private const val TAG = "AgentPreferences"
    }
}
