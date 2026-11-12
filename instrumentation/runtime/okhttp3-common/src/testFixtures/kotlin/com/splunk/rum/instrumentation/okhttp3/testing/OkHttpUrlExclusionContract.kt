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

package com.splunk.rum.instrumentation.okhttp3.testing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Runs the same observable request/span contract through manual and automatic runtime wiring. */
abstract class OkHttpUrlExclusionContract {
    private lateinit var server: MockWebServer
    private lateinit var telemetry: OpenTelemetrySdk
    private val exportedSpans = CopyOnWriteArrayList<SpanData>()
    private val collectingExporter = object : SpanExporter {
        override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
            exportedSpans.addAll(spans)
            return CompletableResultCode.ofSuccess()
        }

        override fun flush() = CompletableResultCode.ofSuccess()

        override fun shutdown() = CompletableResultCode.ofSuccess()
    }
    private val clients = mutableListOf<OkHttpClient>()

    @Volatile
    private var respond: (String) -> MockResponse = { MockResponse() }

    protected abstract fun instrument(
        client: OkHttpClient,
        openTelemetry: OpenTelemetry,
        exclusions: ((String) -> Boolean)?
    ): Call.Factory

    @Before
    fun setUpExclusions() {
        exportedSpans.clear()
        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(collectingExporter))
            .build()
        telemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build()
        respond = { MockResponse() }
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = respond(request.path.orEmpty())
            }
            start()
        }
    }

    @After
    fun tearDownExclusions() {
        clients.forEach {
            it.dispatcher.executorService.shutdownNow()
            it.connectionPool.evictAll()
        }
        server.shutdown()
        telemetry.close()
    }

    protected fun factory(
        exclusions: ((String) -> Boolean)? = { it.contains("/ignored") },
        configure: OkHttpClient.Builder.() -> Unit = {}
    ): Call.Factory {
        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .callTimeout(5, TimeUnit.SECONDS)
            .apply(configure)
            .build()
        clients.add(client)
        return instrument(client, telemetry, exclusions)
    }

    protected fun execute(factory: Call.Factory, path: String) {
        factory.newCall(Request.Builder().url(url(path)).build()).execute().use {
            assertEquals(200, it.code)
        }
    }

    private fun url(path: String): String = server.url(path).toString()

    private fun takeRequest(): RecordedRequest =
        checkNotNull(server.takeRequest(5, TimeUnit.SECONDS)) { "No HTTP request reached MockWebServer" }

    private fun completedUrls(): List<String?> = exportedSpans.map { it.attributes[AttributeKey.stringKey("url.full")] }

    @Test
    fun ignoredRequestExecutesWithoutSpanOrInjectedHeaders() {
        val request = Request.Builder().url(url("/ignored"))
            .header("X-Customer", "preserved").build()
        factory().newCall(request).execute().use { assertEquals(200, it.code) }
        val wire = takeRequest()
        assertEquals("/ignored", wire.path)
        assertEquals("preserved", wire.headers["x-customer"])
        assertNull(wire.headers["traceparent"])
        assertTrue(exportedSpans.isEmpty())
    }

    @Test
    fun noRulesPreserveAllowedSpanAndPropagation() {
        execute(factory(null), "/allowed")
        assertEquals(listOf(url("/allowed")), completedUrls())
        val span = exportedSpans.single()
        assertEquals(200L, span.attributes[AttributeKey.longKey("http.response.status_code")])
        assertTrue(takeRequest().headers["traceparent"].orEmpty().contains(span.spanContext.spanId))
    }

    @Test
    fun configuredNonmatchingRequestStillRecords() {
        execute(factory(), "/allowed")
        assertEquals(listOf(url("/allowed")), completedUrls())
        assertTrue(takeRequest().headers["traceparent"] != null)
    }

    @Test
    fun allowedRequestRetainsParentContextAndPropagatesChildSpan() {
        val parent = telemetry.getTracer("url-exclusion-test").spanBuilder("parent").startSpan()
        try {
            parent.makeCurrent().use {
                execute(factory(), "/allowed")
                assertEquals(parent.spanContext, Span.current().spanContext)
            }

            val child = exportedSpans.single()
            assertEquals(parent.spanContext.traceId, child.spanContext.traceId)
            assertEquals(parent.spanContext.spanId, child.parentSpanId)
            val traceparent = takeRequest().headers["traceparent"]
            assertTrue(traceparent.orEmpty().contains(child.spanContext.spanId))
        } finally {
            parent.end()
        }
    }

    @Test
    fun allowedRedirectToIgnoredRecordsOnlyTheFirstExchange() {
        redirect("/allowed", "/ignored", listOf("/allowed"))
    }

    @Test
    fun ignoredRedirectToAllowedRecordsOnlyTheSecondExchange() {
        redirect("/ignored", "/allowed", listOf("/allowed"))
    }

    @Test
    fun entirelyIgnoredRedirectChainCreatesNoSpan() {
        redirect("/ignored-first", "/ignored-second", emptyList())
    }

    @Test
    fun allowedRedirectChainRetainsSeparateSpans() {
        redirect("/first", "/second", listOf("/first", "/second"))
    }

    @Test
    fun allowedExchangesAroundAnIgnoredRedirectKeepRecording() {
        respond = { path ->
            when (path) {
                "/first" -> MockResponse().setResponseCode(302).addHeader("Location", url("/ignored"))
                "/ignored" -> MockResponse().setResponseCode(302).addHeader("Location", url("/last"))
                else -> MockResponse()
            }
        }
        execute(factory(), "/first")
        assertEquals(listOf(url("/first"), url("/last")), completedUrls())
        assertTrue(takeRequest().headers["traceparent"] != null)
        assertNull(takeRequest().headers["traceparent"])
        assertTrue(takeRequest().headers["traceparent"] != null)
    }

    @Test
    fun ignoredAuthenticationRetryCreatesNoTelemetry() {
        val attempts = AtomicInteger()
        respond = {
            if (attempts.getAndIncrement() == 0) {
                MockResponse().setResponseCode(401).addHeader("WWW-Authenticate", "Basic")
            } else {
                MockResponse()
            }
        }
        val factory = factory {
            authenticator { _, response ->
                response.request.newBuilder().header("Authorization", "test").build()
            }
        }
        execute(factory, "/ignored")
        assertTrue(exportedSpans.isEmpty())
        assertNull(takeRequest().headers["traceparent"])
        assertNull(takeRequest().headers["traceparent"])
        assertEquals(2, attempts.get())
    }

    @Test
    fun ignoredCachedResponseCreatesNoSpan() {
        val directory = Files.createTempDirectory("okhttp-url-exclusion-cache").toFile()
        try {
            Cache(directory, 1024 * 1024).use { cache ->
                respond = { MockResponse().addHeader("Cache-Control", "max-age=600") }
                val factory = factory { cache(cache) }
                execute(factory, "/ignored")
                takeRequest()
                factory.newCall(Request.Builder().url(url("/ignored")).build()).execute().use {
                    assertEquals(200, it.code)
                    assertTrue(it.cacheResponse != null)
                }
                assertEquals(1, server.requestCount)
                assertTrue(exportedSpans.isEmpty())
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun redirect(from: String, to: String, recorded: List<String>) {
        respond = { path ->
            if (path == from) MockResponse().setResponseCode(302).addHeader("Location", url(to)) else MockResponse()
        }
        execute(factory(), from)
        assertEquals(recorded.map(::url), completedUrls())
        listOf(takeRequest(), takeRequest()).forEach { wire ->
            assertEquals(!wire.path.orEmpty().contains("ignored"), wire.headers["traceparent"] != null)
        }
    }

    @Test
    fun rewrittenIgnoredUrlDoesNotCreateAnOriginalUrlSpan() {
        val factory = factory {
            addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().url(url("/ignored")).build())
            }
        }
        execute(factory, "/allowed")
        assertEquals("/ignored", takeRequest().path)
        assertTrue(exportedSpans.isEmpty())
    }

    @Test
    fun ignoredDnsFailureDoesNotCreateTelemetry() {
        dnsFailure("/ignored", expectedSpans = 0)
    }

    @Test
    fun allowedDnsFailureCreatesErrorTelemetry() {
        dnsFailure("/allowed", expectedSpans = 1)
        assertEquals(StatusCode.ERROR, exportedSpans.single().status.statusCode)
    }

    private fun dnsFailure(path: String, expectedSpans: Int) {
        val failure = UnknownHostException("intentional test DNS failure")
        val factory = factory {
            dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> = throw failure
            })
        }
        val thrown = try {
            factory.newCall(Request.Builder().url("http://unresolvable.invalid$path").build()).execute()
            throw AssertionError("Expected DNS failure")
        } catch (error: UnknownHostException) {
            error
        }
        assertSame(failure, thrown)
        assertEquals(expectedSpans, exportedSpans.size)
    }

    @Test
    fun ignoredRequestPreservesCustomerTraceHeaders() {
        val customerTrace = "00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbb-01"
        val request = Request.Builder().url(url("/ignored"))
            .header("traceparent", customerTrace).header("baggage", "customer=value").build()
        factory().newCall(request).execute().close()
        val wire = takeRequest()
        assertEquals(customerTrace, wire.headers["traceparent"])
        assertEquals("customer=value", wire.headers["baggage"])
        assertTrue(exportedSpans.isEmpty())
    }

    @Test
    fun independentFactoriesKeepTheirOwnRules() {
        val first = factory({ it.endsWith("/one") })
        val second = factory({ it.endsWith("/two") })
        execute(first, "/one")
        execute(second, "/one")
        execute(first, "/two")
        execute(second, "/two")
        assertEquals(listOf(url("/one"), url("/two")), completedUrls())
        assertEquals(4, server.requestCount)
    }

    @Test
    fun concurrentCallsDoNotShareFallbackDecisions() {
        val factory = factory()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (0 until 10).map { index ->
                executor.submit {
                    execute(factory, if (index % 2 == 0) "/ignored-$index" else "/allowed-$index")
                }
            }
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
            assertEquals(5, exportedSpans.size)
            assertFalse(completedUrls().any { it.orEmpty().contains("ignored") })
            repeat(10) {
                val wire = takeRequest()
                assertEquals(!wire.path.orEmpty().contains("ignored"), wire.headers["traceparent"] != null)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun ignoredAsyncCallCompletesWithoutTelemetry() {
        val completed = CountDownLatch(1)
        val error = AtomicReference<Throwable>()
        factory().newCall(Request.Builder().url(url("/ignored")).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                error.set(e)
                completed.countDown()
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                completed.countDown()
            }
        })
        assertTrue(completed.await(10, TimeUnit.SECONDS))
        error.get()?.let { throw AssertionError("Async request failed", it) }
        assertEquals("/ignored", takeRequest().path)
        assertTrue(exportedSpans.isEmpty())
    }
}
