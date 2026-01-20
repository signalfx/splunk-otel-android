/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.integration.agent.api

import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class EndpointConfigurationTest {

    @Test
    fun `realm constructor sets token and endpoints correctly`() {
        val config = EndpointConfiguration("us0", "test_token_123")

        assertEquals("us0", config.realm)
        assertEquals("test_token_123", config.rumAccessToken)
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/traces", config.traceEndpoint.toString())
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/logs", config.sessionReplayEndpoint.toString())
    }

    @Test
    fun `single URL constructor extracts token and removes it from URL`() {
        val url = URL("https://rum-ingest.us0.signalfx.com/v1/traces?auth=extracted_token_123")
        val config = EndpointConfiguration(url)

        assertEquals("extracted_token_123", config.rumAccessToken)
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/traces", config.traceEndpoint.toString())
    }

    @Test
    fun `single URL constructor throws exception when no auth parameter`() {
        val url = URL("https://rum-ingest.us0.signalfx.com/v1/traces")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            EndpointConfiguration(url)
        }

        assertNotNull(exception.message)
        assert(exception.message!!.contains("No auth token found"))
    }

    @Test
    fun `single URL constructor throws exception when auth parameter is empty`() {
        val url = URL("https://rum-ingest.us0.signalfx.com/v1/traces?auth=")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            EndpointConfiguration(url)
        }

        assertNotNull(exception.message)
        assert(exception.message!!.contains("No auth token found"))
    }

    @Test
    fun `single URL constructor throws exception when no query string`() {
        val url = URL("https://rum-ingest.us0.signalfx.com/v1/traces")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            EndpointConfiguration(url)
        }

        assertNotNull(exception.message)
        assert(exception.message!!.contains("No auth token found"))
    }

    @Test
    fun `dual URL constructor extracts token from trace URL and removes it`() {
        val traceUrl = URL("https://rum-ingest.us0.signalfx.com/v1/traces?auth=trace_token_123")
        val replayUrl = URL("https://rum-ingest.us0.signalfx.com/v1/logs")

        val config = EndpointConfiguration(traceUrl, replayUrl)

        assertEquals("trace_token_123", config.rumAccessToken)
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/traces", config.traceEndpoint.toString())
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/logs", config.sessionReplayEndpoint.toString())
    }

    @Test
    fun `dual URL constructor extracts token from session replay URL and removes it`() {
        val traceUrl = URL("https://rum-ingest.us0.signalfx.com/v1/traces")
        val replayUrl = URL("https://rum-ingest.us0.signalfx.com/v1/logs?auth=replay_token_123")

        val config = EndpointConfiguration(traceUrl, replayUrl)

        assertEquals("replay_token_123", config.rumAccessToken)
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/traces", config.traceEndpoint.toString())
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/logs", config.sessionReplayEndpoint.toString())
    }

    @Test
    fun `dual URL constructor prefers trace URL token over session replay URL token`() {
        val traceUrl = URL("https://rum-ingest.us0.signalfx.com/v1/traces?auth=trace_token_123")
        val replayUrl = URL("https://rum-ingest.us0.signalfx.com/v1/logs?auth=replay_token_456")

        val config = EndpointConfiguration(traceUrl, replayUrl)

        assertEquals("trace_token_123", config.rumAccessToken)
    }

    @Test
    fun `dual URL constructor throws exception when no auth in either URL`() {
        val traceUrl = URL("https://rum-ingest.us0.signalfx.com/v1/traces")
        val replayUrl = URL("https://rum-ingest.us0.signalfx.com/v1/logs")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            EndpointConfiguration(traceUrl, replayUrl)
        }

        assertNotNull(exception.message)
        assert(exception.message!!.contains("No auth token found"))
    }

    @Test
    fun `URL constructor handles multiple query parameters and only removes auth`() {
        val url = URL("https://rum-ingest.us0.signalfx.com/v1/traces?other=value&auth=multi_param_token&another=param")

        val config = EndpointConfiguration(url)

        assertEquals("multi_param_token", config.rumAccessToken)
        assertEquals(
            "https://rum-ingest.us0.signalfx.com/v1/traces?other=value&another=param",
            config.traceEndpoint.toString()
        )
    }

    @Test
    fun `URL constructor handles auth parameter with special characters`() {
        val url = URL("https://rum-ingest.us0.signalfx.com/v1/traces?auth=token_with-special.chars_123")

        val config = EndpointConfiguration(url)

        assertEquals("token_with-special.chars_123", config.rumAccessToken)
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/traces", config.traceEndpoint.toString())
    }

    @Test
    fun `URL with only auth parameter removes query string entirely`() {
        val url = URL("https://rum-ingest.us0.signalfx.com/v1/traces?auth=only_token_123")

        val config = EndpointConfiguration(url)

        assertEquals("only_token_123", config.rumAccessToken)
        assertEquals("https://rum-ingest.us0.signalfx.com/v1/traces", config.traceEndpoint.toString())
    }
}
