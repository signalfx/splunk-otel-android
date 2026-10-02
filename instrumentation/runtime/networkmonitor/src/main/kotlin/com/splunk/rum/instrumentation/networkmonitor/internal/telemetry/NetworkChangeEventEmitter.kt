/*
 * Copyright 2026 Splunk Inc.
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.networkmonitor.internal.telemetry

import com.splunk.rum.common.logger.Logger as SdkLogger
import com.splunk.rum.instrumentation.networkmonitor.internal.lifecycle.NetworkApplicationStateGate
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger

internal class NetworkChangeEventEmitter(
    private val logger: Logger,
    private val applicationStateGate: NetworkApplicationStateGate
) {
    fun emit(attributes: Attributes) {
        if (!applicationStateGate.canEmit) {
            return
        }

        try {
            logger.logRecordBuilder()
                .setAllAttributes(attributes)
                .setAttribute(EVENT_NAME_KEY, EVENT_NAME)
                .emit()
        } catch (exception: RuntimeException) {
            // Telemetry failures must not escape into the network callback or host app code.
            SdkLogger.w(TAG, "Failed to emit network change event.", exception)
        }
    }

    internal companion object {
        const val EVENT_NAME = "network.change"
        val EVENT_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("event.name")
        val NETWORK_STATUS: AttributeKey<String> = AttributeKey.stringKey("network.status")
        private const val TAG = "NetworkChangeEmitter"
    }
}
