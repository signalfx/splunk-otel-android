/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.networkmonitor

import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_ICC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MCC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MNC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_NAME
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE

internal object NetworkGlobalAttributesUpdater {
    private val keys = listOf(
        NETWORK_CONNECTION_TYPE,
        NETWORK_CONNECTION_SUBTYPE,
        NETWORK_CARRIER_NAME,
        NETWORK_CARRIER_MCC,
        NETWORK_CARRIER_MNC,
        NETWORK_CARRIER_ICC
    )

    fun update(destination: MutableAttributes, source: Attributes) {
        destination.update {
            keys.forEach { key ->
                source[key]?.let { value -> put(key, value) } ?: remove(key)
            }
        }
    }
}
