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

class NetworkGlobalAttributesUpdaterTest {
    private val destination = MutableAttributes()

    @Test
    fun copiesAllNetworkAttributes() {
        val source = Attributes.builder()
            .put(NETWORK_CONNECTION_TYPE, "cell")
            .put(NETWORK_CONNECTION_SUBTYPE, "LTE")
            .put(NETWORK_CARRIER_NAME, "Example")
            .put(NETWORK_CARRIER_MCC, "310")
            .put(NETWORK_CARRIER_MNC, "260")
            .put(NETWORK_CARRIER_ICC, "us")
            .build()

        NetworkGlobalAttributesUpdater.update(destination, source)

        assertEquals("cell", destination[NETWORK_CONNECTION_TYPE])
        assertEquals("LTE", destination[NETWORK_CONNECTION_SUBTYPE])
        assertEquals("Example", destination[NETWORK_CARRIER_NAME])
        assertEquals("310", destination[NETWORK_CARRIER_MCC])
        assertEquals("260", destination[NETWORK_CARRIER_MNC])
        assertEquals("us", destination[NETWORK_CARRIER_ICC])
    }

    @Test
    fun removesStaleOptionalValuesWhenNetworkChanges() {
        NetworkGlobalAttributesUpdater.update(
            destination,
            Attributes.builder()
                .put(NETWORK_CONNECTION_TYPE, "cell")
                .put(NETWORK_CONNECTION_SUBTYPE, "LTE")
                .put(NETWORK_CARRIER_NAME, "Example")
                .put(NETWORK_CARRIER_MCC, "310")
                .put(NETWORK_CARRIER_MNC, "260")
                .put(NETWORK_CARRIER_ICC, "us")
                .build()
        )

        NetworkGlobalAttributesUpdater.update(
            destination,
            Attributes.of(NETWORK_CONNECTION_TYPE, "wifi")
        )

        assertEquals("wifi", destination[NETWORK_CONNECTION_TYPE])
        assertNull(destination[NETWORK_CONNECTION_SUBTYPE])
        assertNull(destination[NETWORK_CARRIER_NAME])
        assertNull(destination[NETWORK_CARRIER_MCC])
        assertNull(destination[NETWORK_CARRIER_MNC])
        assertNull(destination[NETWORK_CARRIER_ICC])
    }

    @Test
    fun removesConnectionTypeWhenSourceDoesNotContainIt() {
        destination[NETWORK_CONNECTION_TYPE] = "wifi"

        NetworkGlobalAttributesUpdater.update(destination, Attributes.empty())

        assertNull(destination[NETWORK_CONNECTION_TYPE])
    }

    @Test
    fun preservesAttributesOwnedByOtherFeatures() {
        val unrelatedKey = AttributeKey.stringKey("unrelated")
        destination[unrelatedKey] = "keep"

        NetworkGlobalAttributesUpdater.update(
            destination,
            Attributes.of(NETWORK_CONNECTION_TYPE, "wifi")
        )

        assertEquals("keep", destination[unrelatedKey])
    }
}
