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
    fun `uses the most specific host and port mapping`() {
        val resolver = PeerServiceMappingResolver(
            mapOf(
                "api.example.test" to "host-service",
                "api.example.test:8443" to "port-service"
            )
        )

        assertEquals("port-service", resolver.resolve("api.example.test", 8443))
    }

    @Test
    fun `ignores path-qualified mappings`() {
        val resolver = PeerServiceMappingResolver(
            mapOf("api.example.test/orders" to "orders-service")
        )

        assertNull(resolver.resolve("api.example.test", 8443))
        assertNull(resolver.resolve("api.example.test", null))
    }

    @Test
    fun `normalizes the absent port sentinel`() {
        val resolver = PeerServiceMappingResolver(
            mapOf("api.example.test" to "orders-service")
        )

        assertEquals("orders-service", resolver.resolve("api.example.test", -1))
    }

    @Test
    fun `ignores malformed and nonmatching mappings without throwing`() {
        val resolver = PeerServiceMappingResolver(
            mapOf(
                "not a valid host" to "invalid",
                "other.example.test:8443" to "other-service"
            )
        )

        assertNull(resolver.resolve("api.example.test", 8443))
    }
}
