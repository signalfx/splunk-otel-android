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

package com.cisco.android.rum.library.okhttp3.tracing

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.util.VirtualField
import okhttp3.Call
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OkHttpCallbackAdviceHelperTest {

    @MockK
    private lateinit var call: Call

    @MockK(relaxed = true)
    private lateinit var request: Request

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var rootContext: Context

    @MockK(relaxed = true)
    private lateinit var virtualField: VirtualField<Request, Context>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(Context::class)
        mockkStatic(VirtualField::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `propagateContext should return true and set context for request when shouldPropagateContext is true`() {
        every { Context.current() } returns context
        every { Context.root() } returns rootContext
        every { call.request() } returns request
        every {
            VirtualField.find(
                Request::class.java,
                Context::class.java
            )
        } returns virtualField

        val result = OkHttpCallbackAdviceHelper.propagateContext(call)

        verify { virtualField.set(request, context) }
        assertTrue(result)
    }

    @Test
    fun `propagateContext should return false when shouldPropagateContext is false`() {
        every { Context.current() } returns context
        every { Context.root() } returns context

        val result = OkHttpCallbackAdviceHelper.propagateContext(call)

        assertFalse(result)
    }

    @Test
    fun `tryRecoverPropagatedContextFromCallback should return context associated with request`() {
        every {
            VirtualField.find(
                Request::class.java,
                Context::class.java
            )
        } returns virtualField
        every { virtualField.get(request) } returns context

        val result = OkHttpCallbackAdviceHelper.tryRecoverPropagatedContextFromCallback(request)

        assertEquals(context, result)
    }
}
