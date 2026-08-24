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

package com.splunk.rum.instrumentation.httpurlconnection.auto

import com.splunk.rum.instrumentation.httpurlconnection.auto.internal.HttpUrlConnectionSingletons
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HttpUrlInstrumentationTest {
    private val exportedSpans = mutableListOf<SpanData>()
    private lateinit var tracerProvider: SdkTracerProvider

    @Before
    fun setUp() {
        val exporter = object : SpanExporter {
            override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
                exportedSpans += spans
                return CompletableResultCode.ofSuccess()
            }

            override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

            override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
        }
        tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build()
        val openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.noop())
            .build()

        HttpUrlInstrumentation().apply {
            capturedRequestHeaders = listOf("x-request-id")
            capturedResponseHeaders = listOf("x-response-id")
            setPeerServiceMapping(mapOf("api.example.test:8443/orders" to "orders-service"))
            install(openTelemetry)
        }
    }

    @After
    fun tearDown() {
        tracerProvider.shutdown().join(10, TimeUnit.SECONDS)
    }

    @Test
    fun `records the network span attribute matrix and path peer mapping`() {
        val connection = StubHttpURLConnection(
            URL("https://api.example.test:8443/orders/42"),
            requestMethod = "POST",
            requestHeaders = mapOf("x-request-id" to "request-123"),
            responseHeaders = mapOf("x-response-id" to "response-456")
        )

        endSpan(connection, responseCode = 503)

        val span = exportedSpans.single()
        assertEquals("POST", span.name)
        assertEquals("POST", span.attributes.get(AttributeKey.stringKey("http.request.method")))
        assertEquals(503L, span.attributes.get(AttributeKey.longKey("http.response.status_code")))
        assertEquals("api.example.test", span.attributes.get(AttributeKey.stringKey("server.address")))
        assertEquals(8443L, span.attributes.get(AttributeKey.longKey("server.port")))
        assertEquals(
            listOf("request-123"),
            span.attributes.get(AttributeKey.stringArrayKey("http.request.header.x-request-id"))
        )
        assertEquals(
            listOf("response-456"),
            span.attributes.get(AttributeKey.stringArrayKey("http.response.header.x-response-id"))
        )
        assertEquals("orders-service", span.attributes.get(AttributeKey.stringKey("peer.service")))
        assertEquals("503", span.attributes.get(AttributeKey.stringKey("error.type")))
        assertEquals(StatusCode.ERROR, span.status.statusCode)
    }

    @Test
    fun `records connection errors`() {
        val error = IOException("connection failed")
        val connection = StubHttpURLConnection(URL("https://api.example.test:8443/orders/42"))

        endSpan(connection, responseCode = -1, error = error)

        val span = exportedSpans.single()
        assertEquals(StatusCode.ERROR, span.status.statusCode)
        assertEquals(IOException::class.java.name, span.attributes.get(AttributeKey.stringKey("error.type")))
        assertEquals(IOException::class.java.name, span.events.single().attributes.get(EXCEPTION_TYPE))
    }

    private fun endSpan(connection: HttpURLConnection, responseCode: Int, error: Throwable? = null) {
        val instrumenter = checkNotNull(HttpUrlConnectionSingletons.instrumenter())
        val context = instrumenter.start(Context.root(), connection)
        instrumenter.end(context, connection, responseCode, error)
    }

    private class StubHttpURLConnection(
        url: URL,
        requestMethod: String = "GET",
        private val requestHeaders: Map<String, String> = emptyMap(),
        private val responseHeaders: Map<String, String> = emptyMap()
    ) : HttpURLConnection(url) {
        init {
            method = requestMethod
        }

        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun getRequestProperty(key: String): String? = requestHeaders[key]

        override fun getHeaderField(name: String): String? = responseHeaders[name]
    }

    private companion object {
        private val EXCEPTION_TYPE = AttributeKey.stringKey("exception.type")
    }
}
