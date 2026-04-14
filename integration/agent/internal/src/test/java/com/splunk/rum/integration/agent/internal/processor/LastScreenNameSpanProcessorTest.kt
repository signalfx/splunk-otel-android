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
import com.splunk.rum.integration.agent.internal.attributes.IScreenNameTracker
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class LastScreenNameSpanProcessorTest {

    private val tracker = object : IScreenNameTracker {
        override var lastScreenName: String? = "Menu"
        override var screenName: String = "CrashReportsFragment"
    }

    private val processor = LastScreenNameSpanProcessor(tracker)

    @Test
    fun `sets last screen name on AppStart span`() {
        val span = mockSpan(GlobalRumConstants.APP_START_SPAN_NAME)

        processor.onStart(Context.root(), span)

        verify(span).setAttribute(GlobalRumConstants.LAST_SCREEN_NAME_KEY, "Menu")
    }

    @Test
    fun `does not set last screen name on navigation event span`() {
        val span = mockSpan("app.ui.navigation")

        processor.onStart(Context.root(), span)

        verify(span, never()).setAttribute(
            GlobalRumConstants.LAST_SCREEN_NAME_KEY,
            "Menu"
        )
    }

    @Test
    fun `does not set last screen name on lifecycle span`() {
        val span = mockSpan("app.ui.lifecycle")

        processor.onStart(Context.root(), span)

        verify(span, never()).setAttribute(
            GlobalRumConstants.LAST_SCREEN_NAME_KEY,
            "Menu"
        )
    }

    @Test
    fun `does not set attribute when lastScreenName is null on cold start`() {
        tracker.lastScreenName = null
        val span = mockSpan(GlobalRumConstants.APP_START_SPAN_NAME)

        processor.onStart(Context.root(), span)

        verify(span, never()).setAttribute(
            GlobalRumConstants.LAST_SCREEN_NAME_KEY,
            "Menu"
        )
    }

    @Test
    fun `isStartRequired returns true`() {
        assert(processor.isStartRequired())
    }

    @Test
    fun `isEndRequired returns false`() {
        assert(!processor.isEndRequired())
    }

    private fun mockSpan(name: String): ReadWriteSpan {
        val span = mock(ReadWriteSpan::class.java)
        `when`(span.name).thenReturn(name)
        return span
    }
}
