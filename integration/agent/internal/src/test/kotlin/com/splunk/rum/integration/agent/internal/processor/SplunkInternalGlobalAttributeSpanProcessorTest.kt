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

package com.splunk.rum.integration.agent.internal.processor

import com.splunk.rum.common.otel.internal.GlobalRumConstants
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SplunkInternalGlobalAttributeSpanProcessorTest {

    private val processor = SplunkInternalGlobalAttributeSpanProcessor()

    @Before
    fun setup() {
        SplunkInternalGlobalAttributeSpanProcessor.attributes[GlobalRumConstants.SCREEN_NAME_KEY] = "CurrentScreen"
    }

    @After
    fun teardown() {
        SplunkInternalGlobalAttributeSpanProcessor.attributes[GlobalRumConstants.SCREEN_NAME_KEY] =
            GlobalRumConstants.DEFAULT_SCREEN_NAME
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `sets screen name on span that has none`() {
        val span = mockSpan(name = "SomeHttpSpan", existingScreenName = null)

        processor.onStart(Context.root(), span)

        verify(span).setAttribute(
            GlobalRumConstants.SCREEN_NAME_KEY as AttributeKey<Any>,
            "CurrentScreen" as Any
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `does not overwrite screen name on navigation span`() {
        val span = mockSpan(name = "app.ui.navigation", existingScreenName = "SnapshotScreen")

        processor.onStart(Context.root(), span)

        verify(span, never()).setAttribute(
            GlobalRumConstants.SCREEN_NAME_KEY as AttributeKey<Any>,
            "CurrentScreen" as Any
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `does not overwrite screen name on lifecycle span`() {
        val span = mockSpan(name = "app.ui.lifecycle", existingScreenName = "SnapshotScreen")

        processor.onStart(Context.root(), span)

        verify(span, never()).setAttribute(
            GlobalRumConstants.SCREEN_NAME_KEY as AttributeKey<Any>,
            "CurrentScreen" as Any
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `does not overwrite screen name on any span type that already has it`() {
        val span = mockSpan(name = "HTTP GET", existingScreenName = "SnapshotScreen")

        processor.onStart(Context.root(), span)

        verify(span, never()).setAttribute(
            GlobalRumConstants.SCREEN_NAME_KEY as AttributeKey<Any>,
            "CurrentScreen" as Any
        )
    }

    @Test
    fun `isStartRequired returns true`() {
        assert(processor.isStartRequired())
    }

    @Test
    fun `isEndRequired returns true`() {
        assert(processor.isEndRequired())
    }

    private fun mockSpan(name: String, existingScreenName: String?): ReadWriteSpan {
        val span = mock(ReadWriteSpan::class.java)
        `when`(span.name).thenReturn(name)
        `when`(span.getAttribute(GlobalRumConstants.SCREEN_NAME_KEY)).thenReturn(existingScreenName)
        return span
    }
}
