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

package com.splunk.rum.agent.common.otel.span

import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.trace.data.SpanData
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class DurableSpanPersistenceCoordinatorTest {

    @After
    fun tearDown() {
        DurableSpanPersistenceCoordinator.resetForTest()
    }

    @Test
    fun `acknowledges only the registered span in a successful disk batch`() {
        val registeredSpanId = "0123456789abcdef"
        assertTrue(DurableSpanPersistenceCoordinator.register(registeredSpanId))

        DurableSpanPersistenceCoordinator.complete(
            listOf(spanData("fedcba9876543210"), spanData(registeredSpanId)),
            success = true
        )

        assertTrue(DurableSpanPersistenceCoordinator.await(registeredSpanId, 10))
    }

    @Test
    fun `reports failure when the containing batch was not written`() {
        val spanId = "0123456789abcdef"
        assertTrue(DurableSpanPersistenceCoordinator.register(spanId))

        DurableSpanPersistenceCoordinator.complete(listOf(spanData(spanId)), success = false)

        assertFalse(DurableSpanPersistenceCoordinator.await(spanId, 10))
    }

    @Test
    fun `times out and consumes an unacknowledged registration`() {
        val spanId = "0123456789abcdef"
        assertTrue(DurableSpanPersistenceCoordinator.register(spanId))

        assertFalse(DurableSpanPersistenceCoordinator.await(spanId, 0))
        assertFalse(DurableSpanPersistenceCoordinator.await(spanId, 10))
    }

    private fun spanData(spanId: String): SpanData {
        val spanData = mock(SpanData::class.java)
        val context = SpanContext.create(
            "0123456789abcdef0123456789abcdef",
            spanId,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        )
        `when`(spanData.spanContext).thenReturn(context)
        return spanData
    }
}
