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

package com.splunk.rum.common.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoredEndpointConfigTest {

    @Test
    fun `roundtrip with all fields`() {
        val config = StoredEndpointConfig(
            tracesBaseUrl = "https://rum-ingest.us0.signalfx.com/v1/traces",
            logsBaseUrl = "https://rum-ingest.us0.signalfx.com/v1/logs",
            rumAccessToken = "my-token-123"
        )

        val json = config.toJson()
        val restored = StoredEndpointConfig.fromJson(json)

        assertNotNull(restored)
        assertEquals(config, restored)
    }

    @Test
    fun `roundtrip with null logsBaseUrl`() {
        val config = StoredEndpointConfig(
            tracesBaseUrl = "https://example.com/v1/traces",
            logsBaseUrl = null,
            rumAccessToken = "token"
        )

        val json = config.toJson()
        val restored = StoredEndpointConfig.fromJson(json)

        assertNotNull(restored)
        assertEquals(config, restored)
        assertNull(restored!!.logsBaseUrl)
    }

    @Test
    fun `roundtrip with null rumAccessToken`() {
        val config = StoredEndpointConfig(
            tracesBaseUrl = "https://example.com/v1/traces",
            logsBaseUrl = "https://example.com/v1/logs",
            rumAccessToken = null
        )

        val json = config.toJson()
        val restored = StoredEndpointConfig.fromJson(json)

        assertNotNull(restored)
        assertEquals(config, restored)
        assertNull(restored!!.rumAccessToken)
    }

    @Test
    fun `roundtrip with all nullable fields null`() {
        val config = StoredEndpointConfig(
            tracesBaseUrl = "https://example.com/v1/traces",
            logsBaseUrl = null,
            rumAccessToken = null
        )

        val json = config.toJson()
        val restored = StoredEndpointConfig.fromJson(json)

        assertNotNull(restored)
        assertEquals(config, restored)
    }

    @Test
    fun `fromJson returns null for invalid json`() {
        assertNull(StoredEndpointConfig.fromJson("not json"))
    }

    @Test
    fun `fromJson returns null for empty string`() {
        assertNull(StoredEndpointConfig.fromJson(""))
    }

    @Test
    fun `fromJson returns null when tracesBaseUrl is missing`() {
        val json = """{"logsBaseUrl":"https://example.com","rumAccessToken":"tok"}"""
        assertNull(StoredEndpointConfig.fromJson(json))
    }
}
