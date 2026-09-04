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

package com.splunk.rum.instrumentation.okhttp3.auto

import com.splunk.rum.instrumentation.okhttp3.auto.internal.OkHttpSingletons
import com.splunk.rum.instrumentation.okhttp3.auto.internal.PeerServiceAttributesExtractor
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesGetter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class OkHttpInstrumentationTest {
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

        OkHttpInstrumentation().apply {
            capturedRequestHeaders = listOf("x-request-id")
            capturedResponseHeaders = listOf("x-response-id")
            setPeerServiceMapping(
                mapOf(
                    "api.example.test" to "host-service",
                    "api.example.test:8443" to "checkout-service",
                    "api.example.test:8443/orders" to "orders-service"
                )
            )
            install(openTelemetry)
        }
    }

    @After
    fun tearDown() {
        tracerProvider.shutdown().join(10, TimeUnit.SECONDS)
    }

    @Test
    fun validateDefaultHttpMethods() {
        val instrumentation = OkHttpInstrumentation()
        assertEquals(
            setOf("CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT", "TRACE"),
            instrumentation.knownMethods
        )
    }

    @Test
    fun `records the network span attribute matrix and peer mapping`() {
        val request = Request.Builder()
            .url("https://api.example.test:8443/orders")
            .post(byteArrayOf().toRequestBody())
            .header("x-request-id", "request-123")
            .build()
        val chain = FakeChain(request, responseCode = 503, responseHeader = "response-456")

        OkHttpSingletons.tracingInterceptor.intercept(chain)

        val span = exportedSpans.single()
        assertEquals("POST", span.name)
        assertEquals(BuildConfig.VERSION_NAME, span.instrumentationScopeInfo.version)
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
    fun `does not read request attributes when peer service mapping is empty`() {
        val extractor = PeerServiceAttributesExtractor(
            object : ServerAttributesGetter<Interceptor.Chain> {
                override fun getServerAddress(request: Interceptor.Chain): String = error("must not be called")

                override fun getServerPort(request: Interceptor.Chain): Int = error("must not be called")
            },
            emptyMap()
        )
        val attributes = Attributes.builder()

        extractor.onEnd(
            attributes,
            Context.root(),
            FakeChain(Request.Builder().url("https://api.example.test/orders").build()),
            null,
            null
        )

        assertEquals(Attributes.empty(), attributes.build())
    }

    @Test
    fun `records request failures and rethrows the original error`() {
        val expected = IOException("connection failed")
        val chain = FakeChain(
            Request.Builder().url("https://api.example.test:8443/orders").build(),
            error = expected
        )

        val actual = assertThrows(IOException::class.java) {
            OkHttpSingletons.tracingInterceptor.intercept(chain)
        }

        assertEquals(expected, actual)
        val span = exportedSpans.single()
        assertEquals(StatusCode.ERROR, span.status.statusCode)
        assertEquals(IOException::class.java.name, span.attributes.get(AttributeKey.stringKey("error.type")))
        assertEquals(IOException::class.java.name, span.events.single().attributes.get(EXCEPTION_TYPE))
    }

    private class FakeChain(
        private val originalRequest: Request,
        private val responseCode: Int = 200,
        private val responseHeader: String? = null,
        private val error: IOException? = null
    ) : Interceptor.Chain {
        override fun request(): Request = originalRequest

        override fun proceed(request: Request): Response {
            error?.let { throw it }
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(responseCode)
                .message("test response")
                .apply { responseHeader?.let { header("x-response-id", it) } }
                .build()
        }

        override fun connection(): Connection? = null

        override fun call(): Call = throw UnsupportedOperationException()

        override fun connectTimeoutMillis(): Int = 0

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun readTimeoutMillis(): Int = 0

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 0

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    private companion object {
        private val EXCEPTION_TYPE = AttributeKey.stringKey("exception.type")
    }
}
