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

package com.splunk.rum.agent.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeerServiceMappingResolverTest {

    @Test
    fun `uses the most specific host port and path mapping`() {
        val resolver = PeerServiceMappingResolver(
            mapOf(
                "api.example.test" to "host-service",
                "api.example.test:8443" to "port-service",
                "api.example.test:8443/orders" to "orders-service"
            )
        )

        assertEquals(
            "orders-service",
            resolver.resolve("api.example.test", 8443, "/orders/42")
        )
    }

    @Test
    fun `matches host and port mappings when no request path is available`() {
        val resolver = PeerServiceMappingResolver(
            mapOf(
                "api.example.test" to "host-service",
                "api.example.test:8443" to "port-service",
                "api.example.test:8443/orders" to "orders-service"
            )
        )

        assertEquals("port-service", resolver.resolve("api.example.test", 8443, null))
    }

    @Test
    fun `does not match a portless path mapping for a request with an explicit port`() {
        val resolver = PeerServiceMappingResolver(
            mapOf("api.example.test/orders" to "orders-service")
        )

        assertNull(resolver.resolve("api.example.test", 8443, "/orders/42"))
        assertEquals("orders-service", resolver.resolve("api.example.test", null, "/orders/42"))
    }

    @Test
    fun `ignores malformed and nonmatching mappings without throwing`() {
        val resolver = PeerServiceMappingResolver(
            mapOf(
                "not a valid host" to "invalid",
                "other.example.test:8443/orders" to "other-service"
            )
        )

        assertNull(resolver.resolve("api.example.test", 8443, "/orders/42"))
    }
}
