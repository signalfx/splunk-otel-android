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

package com.splunk.rum.instrumentation.networkmonitor.internal.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentNetworkTest {
    @Test
    fun projectsExpectedNetworkStateValues() {
        assertEquals("unavailable", NetworkState.NO_NETWORK_AVAILABLE.humanName)
        assertEquals("cell", NetworkState.TRANSPORT_CELLULAR.humanName)
        assertEquals("wifi", NetworkState.TRANSPORT_WIFI.humanName)
        assertEquals("wired", NetworkState.TRANSPORT_WIRED.humanName)
        assertEquals("unknown", NetworkState.TRANSPORT_UNKNOWN.humanName)
        assertEquals("vpn", NetworkState.TRANSPORT_VPN.humanName)
    }

    @Test
    fun projectsExpectedCarrierValues() {
        val network = CurrentNetwork(
            NetworkState.TRANSPORT_CELLULAR,
            carrier = Carrier(7, "Carrier", "123", "45", "gb")
        )

        assertEquals("Carrier", network.carrierName)
        assertEquals("123", network.carrierCountryCode)
        assertEquals("45", network.carrierNetworkCode)
        assertEquals("gb", network.carrierIsoCountryCode)
    }

    @Test
    fun missingCarrierProjectsNullValues() {
        val network = CurrentNetwork(NetworkState.TRANSPORT_WIFI)

        assertNull(network.carrierName)
        assertNull(network.carrierCountryCode)
        assertNull(network.carrierNetworkCode)
        assertNull(network.carrierIsoCountryCode)
    }
}
