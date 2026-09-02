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

package com.splunk.rum.instrumentation.httpurlconnection.auto.internal

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeerServiceAttributesExtractorTest {

    @Test
    fun `does not read request attributes when peer service mapping is empty`() {
        val extractor = PeerServiceAttributesExtractor(
            NoAccessHttpAttributesGetter,
            emptyMap()
        )
        val attributes = Attributes.builder()

        extractor.onEnd(
            attributes,
            Context.root(),
            StubHttpURLConnection(URL("https://api.example.test/orders/42")),
            200,
            null
        )

        assertEquals(Attributes.empty(), attributes.build())
    }

    @Test
    fun `uses the most specific host port and path mapping`() {
        val extractor = PeerServiceAttributesExtractor(
            HttpUrlHttpAttributesGetter,
            mapOf(
                "api.example.test" to "host-service",
                "api.example.test:8443" to "port-service",
                "api.example.test:8443/orders" to "orders-service"
            )
        )
        val attributes = Attributes.builder()

        extractor.onEnd(
            attributes,
            Context.root(),
            StubHttpURLConnection(URL("https://api.example.test:8443/orders/42")),
            200,
            null
        )

        assertEquals("orders-service", attributes.build().get(PEER_SERVICE))
    }

    @Test
    fun `ignores malformed and nonmatching mappings without throwing`() {
        val extractor = PeerServiceAttributesExtractor(
            HttpUrlHttpAttributesGetter,
            mapOf(
                "not a valid host" to "invalid",
                "other.example.test:8443/orders" to "other-service"
            )
        )
        val attributes = Attributes.builder()

        extractor.onEnd(
            attributes,
            Context.root(),
            StubHttpURLConnection(URL("https://api.example.test:8443/orders/42")),
            200,
            null
        )

        assertNull(attributes.build().get(PEER_SERVICE))
    }

    private class StubHttpURLConnection(url: URL) : HttpURLConnection(url) {
        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false
    }

    private object NoAccessHttpAttributesGetter : HttpClientAttributesGetter<URLConnection, Int> {
        override fun getHttpRequestMethod(request: URLConnection): String = error("must not be called")

        override fun getHttpRequestHeader(request: URLConnection, name: String): List<String> =
            error("must not be called")

        override fun getHttpResponseStatusCode(request: URLConnection, response: Int, error: Throwable?): Int =
            error("must not be called")

        override fun getHttpResponseHeader(request: URLConnection, response: Int, name: String): List<String> =
            error("must not be called")

        override fun getUrlFull(request: URLConnection): String = error("must not be called")

        override fun getServerAddress(request: URLConnection): String = error("must not be called")

        override fun getServerPort(request: URLConnection): Int = error("must not be called")
    }

    private companion object {
        private val PEER_SERVICE = AttributeKey.stringKey("peer.service")
    }
}
