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

package com.splunk.rum.agent.common.otel.http

import com.splunk.rum.agent.common.storage.StoredEndpointConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthHeaderBuilderTest {

    private val logTag = "TestTag"

    @Test
    fun `buildHeaders includes auth token when available`() {
        val config = StoredEndpointConfig(
            tracesBaseUrl = "https://example.com/v1/traces",
            sessionReplayBaseUrl = null,
            rumAccessToken = "test-token-123"
        )

        val headers = AuthHeaderBuilder.buildHeaders(config, logTag)

        assertEquals(2, headers.size)
        assertTrue(headers.any { it.name == "Content-Type" && it.value == "application/x-protobuf" })
        assertTrue(headers.any { it.name == "X-SF-Token" && it.value == "test-token-123" })
    }

    @Test
    fun `buildHeaders does not crash when token is null`() {
        val config = StoredEndpointConfig(
            tracesBaseUrl = "https://example.com/v1/traces",
            sessionReplayBaseUrl = null,
            rumAccessToken = null
        )

        val headers = AuthHeaderBuilder.buildHeaders(config, logTag)

        assertEquals(1, headers.size)
        assertEquals("Content-Type", headers[0].name)
        assertEquals("application/x-protobuf", headers[0].value)
    }

    @Test
    fun `buildHeaders always includes Content-Type header`() {
        val config = StoredEndpointConfig(
            tracesBaseUrl = "https://example.com/v1/traces",
            sessionReplayBaseUrl = null,
            rumAccessToken = null
        )

        val headers = AuthHeaderBuilder.buildHeaders(config, logTag)

        assertTrue(headers.any { it.name == "Content-Type" && it.value == "application/x-protobuf" })
    }

    @Test
    fun `buildHeaders does not include X-SF-Token when token is null`() {
        val config = StoredEndpointConfig(
            tracesBaseUrl = "https://example.com/v1/traces",
            sessionReplayBaseUrl = null,
            rumAccessToken = null
        )

        val headers = AuthHeaderBuilder.buildHeaders(config, logTag)

        assertTrue(headers.none { it.name == "X-SF-Token" })
    }

    @Test
    fun `buildHeaders treats empty string token same as null`() {
        val config = StoredEndpointConfig(
            tracesBaseUrl = "https://example.com/v1/traces",
            sessionReplayBaseUrl = null,
            rumAccessToken = ""
        )

        val headers = AuthHeaderBuilder.buildHeaders(config, logTag)

        assertEquals(1, headers.size)
        assertTrue(headers.none { it.name == "X-SF-Token" })
    }
}
