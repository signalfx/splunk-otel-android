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

package com.splunk.rum.instrumentation.okhttp3

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.context.Context
import io.opentelemetry.context.ContextKey
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Test

class TracingCallFactoryTest {

    private val contextKeyA = ContextKey.named<String>("tracing-call-factory-test-a")
    private val contextKeyB = ContextKey.named<String>("tracing-call-factory-test-b")

    private val client = OkHttpClient.Builder().build()
    private val factory = OkHttpTelemetry.builder(OpenTelemetry.noop())
        .build()
        .newCallFactory(client)

    @Test
    fun newCall_storesCurrentContextOnRequest() {
        val contextA = Context.current().with(contextKeyA, "screen-a")
        val request = Request.Builder().url("https://example.com").build()

        val call = contextA.makeCurrent().use {
            factory.newCall(request)
        }

        assertEquals(contextA, TracingCallFactory.getCallingContextForRequest(call.request()))
    }

    @Test
    fun clone_updatesStoredRequestContextToCurrentContext() {
        val contextA = Context.current().with(contextKeyA, "screen-a")
        val contextB = Context.current().with(contextKeyB, "screen-b")
        val request = Request.Builder().url("https://example.com").build()

        val originalCall = contextA.makeCurrent().use {
            factory.newCall(request)
        }

        val clonedCall = contextB.makeCurrent().use {
            originalCall.clone()
        }

        assertNotSame(originalCall, clonedCall)
        assertEquals(contextA, TracingCallFactory.getCallingContextForRequest(originalCall.request()))
        assertEquals(contextB, TracingCallFactory.getCallingContextForRequest(clonedCall.request()))
        assertNotEquals(
            TracingCallFactory.getCallingContextForRequest(originalCall.request()),
            TracingCallFactory.getCallingContextForRequest(clonedCall.request())
        )
        assertNotNull(clonedCall.request().tag(Context::class.java))
    }
}
