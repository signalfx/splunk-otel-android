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

import com.splunk.rum.instrumentation.networkmonitor.internal.model.Carrier
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import com.splunk.rum.instrumentation.networkmonitor.internal.model.NetworkState
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_ICC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MCC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MNC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_NAME
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentNetworkAttributesTest {
    @Test
    fun buildsAttributesForLostNetwork() {
        val network = CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE)

        val attributes = CurrentNetworkAttributes.extract(network)

        assertEquals("lost", attributes[NetworkChangeEventEmitter.NETWORK_STATUS])
        assertEquals("unavailable", attributes[NETWORK_CONNECTION_TYPE])
    }

    @Test
    fun buildsAttributesForAvailableNetwork() {
        val network = CurrentNetwork(NetworkState.TRANSPORT_WIFI)

        val attributes = CurrentNetworkAttributes.extract(network)

        assertEquals("available", attributes[NetworkChangeEventEmitter.NETWORK_STATUS])
        assertEquals("wifi", attributes[NETWORK_CONNECTION_TYPE])
    }

    @Test
    fun includesNetworkAndCarrierDetails() {
        val network = CurrentNetwork(
            state = NetworkState.TRANSPORT_CELLULAR,
            subType = "LTE",
            carrier = Carrier(
                id = 42,
                name = "Example",
                mobileCountryCode = "310",
                mobileNetworkCode = "260",
                isoCountryCode = "us"
            )
        )

        val attributes = CurrentNetworkAttributes.extract(network)

        assertEquals("cell", attributes[NETWORK_CONNECTION_TYPE])
        assertEquals("LTE", attributes[NETWORK_CONNECTION_SUBTYPE])
        assertEquals("Example", attributes[NETWORK_CARRIER_NAME])
        assertEquals("310", attributes[NETWORK_CARRIER_MCC])
        assertEquals("260", attributes[NETWORK_CARRIER_MNC])
        assertEquals("us", attributes[NETWORK_CARRIER_ICC])
    }

    @Test
    fun omitsMissingOptionalDetails() {
        val attributes = CurrentNetworkAttributes.extract(
            CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN)
        )

        assertEquals("unknown", attributes[NETWORK_CONNECTION_TYPE])
        assertNull(attributes[NETWORK_CONNECTION_SUBTYPE])
        assertNull(attributes[NETWORK_CARRIER_NAME])
        assertNull(attributes[NETWORK_CARRIER_MCC])
        assertNull(attributes[NETWORK_CARRIER_MNC])
        assertNull(attributes[NETWORK_CARRIER_ICC])
    }

    @Test
    fun unknownTransportIsAvailableRatherThanLost() {
        val attributes = CurrentNetworkAttributes.extract(
            CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN)
        )

        assertEquals("available", attributes[NetworkChangeEventEmitter.NETWORK_STATUS])
    }

    @Test
    fun addingNetworkDetailsPreservesExistingAttributes() {
        val existingKey = AttributeKey.stringKey("existing")
        val builder = Attributes.builder().put(existingKey, "value")

        val attributes = CurrentNetworkAttributes.addTo(
            builder,
            CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        ).build()

        assertEquals("value", attributes[existingKey])
        assertEquals("wifi", attributes[NETWORK_CONNECTION_TYPE])
    }
}
