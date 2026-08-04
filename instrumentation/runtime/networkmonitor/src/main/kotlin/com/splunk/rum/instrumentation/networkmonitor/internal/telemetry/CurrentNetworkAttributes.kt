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

import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import com.splunk.rum.instrumentation.networkmonitor.internal.model.NetworkState
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_ICC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MCC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MNC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_NAME
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE

internal object CurrentNetworkAttributes {
    fun extract(currentNetwork: CurrentNetwork): Attributes = addTo(Attributes.builder(), currentNetwork)
        .put(
            NetworkChangeEventEmitter.NETWORK_STATUS,
            if (currentNetwork.state == NetworkState.NO_NETWORK_AVAILABLE) "lost" else "available"
        )
        .build()

    fun addTo(builder: AttributesBuilder, currentNetwork: CurrentNetwork): AttributesBuilder = builder.apply {
        put(NETWORK_CONNECTION_TYPE, currentNetwork.state.humanName)
        currentNetwork.subType?.let { put(NETWORK_CONNECTION_SUBTYPE, it) }
        currentNetwork.carrierName?.let { put(NETWORK_CARRIER_NAME, it) }
        currentNetwork.carrierCountryCode?.let { put(NETWORK_CARRIER_MCC, it) }
        currentNetwork.carrierNetworkCode?.let { put(NETWORK_CARRIER_MNC, it) }
        currentNetwork.carrierIsoCountryCode?.let { put(NETWORK_CARRIER_ICC, it) }
    }
}
