/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.rum.library.okhttp3.tracing

import com.splunk.rum.library.okhttp3.tracing.OkHttpInterceptorUtils
import com.splunk.rum.library.okhttp3.tracing.TracingInterceptor
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifySequence
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class TracingInterceptorTest {

    @MockK
    private lateinit var instrumenter: Instrumenter<Request, Response>

    @MockK
    private lateinit var propagators: ContextPropagators

    @MockK
    private lateinit var interceptor: TracingInterceptor

    @MockK
    private lateinit var chain: Interceptor.Chain

    @MockK(relaxed = true)
    private lateinit var request: Request

    @MockK
    private lateinit var response: Response

    @MockK
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var scope: Scope

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        interceptor = TracingInterceptor(instrumenter, propagators)

        mockkStatic(OkHttpInterceptorUtils::class)
        mockkStatic(Context::class)

        every { chain.request() } returns request
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `intercept should proceed without tracing when tracing is disabled`() {
        every { OkHttpInterceptorUtils.isTracingEnabledAtInitiation(any(), any()) } returns false
        every { chain.proceed(request) } returns response

        val result = interceptor.intercept(chain)

        assertEquals(response, result)
        verify(exactly = 1) { chain.proceed(request) }
        verify(exactly = 0) { instrumenter.shouldStart(any(), any()) }
    }

    @Test
    fun `intercept should proceed without tracing when instrumenter shouldStart returns false`() {
        every { OkHttpInterceptorUtils.isTracingEnabledAtInitiation(any(), any()) } returns true
        every { instrumenter.shouldStart(any(), request) } returns false
        every { chain.proceed(request) } returns response

        val result = interceptor.intercept(chain)

        assertEquals(response, result)
        verify(exactly = 1) { chain.proceed(request) }
        verify(exactly = 0) { instrumenter.start(any(), any()) }
    }

    @Test
    fun `intercept should start and end instrumenter when tracing is enabled and instrumenter shouldStart returns true`() {
        setUpForTracing()
        every { chain.proceed(any()) } returns response

        val result = interceptor.intercept(chain)

        assertEquals(response, result)
        verifySequence {
            chain.request()
            instrumenter.shouldStart(any(), request)
            instrumenter.start(any(), request)
            propagators.textMapPropagator.inject(context, any<Builder>(), any())
            chain.proceed(any())
            instrumenter.end(context, any(), response, null)
        }
    }

    @Test(expected = IOException::class)
    fun `intercept should end instrumenter with error when an exception occurs`() {
        setUpForTracing()
        every { chain.proceed(any()) } throws IOException()

        try {
            interceptor.intercept(chain)
        } catch (e: IOException) {
            verify(exactly = 1) {
                instrumenter.end(
                    context,
                    any(),
                    null,
                    ofType(IOException::class)
                )
            }
            throw e
        }
    }

    private fun setUpForTracing() {
        every { OkHttpInterceptorUtils.isTracingEnabledAtInitiation(any(), any()) } returns true
        every { instrumenter.shouldStart(any(), request) } returns true
        every { instrumenter.start(any(), request) } returns context
        every { context.makeCurrent() } returns scope
        every {
            propagators.textMapPropagator.inject(context, any<Builder>(), any())
        } just Runs
        every { instrumenter.end(any(), any(), any(), any()) } just Runs
    }
}