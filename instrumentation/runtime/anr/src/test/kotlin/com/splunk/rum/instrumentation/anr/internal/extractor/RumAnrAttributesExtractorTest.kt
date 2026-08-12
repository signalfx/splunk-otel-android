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

package com.splunk.rum.instrumentation.anr.internal.extractor

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants
import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RumAnrAttributesExtractorTest {

    private lateinit var extractor: RumAnrAttributesExtractor

    @Before
    fun setUp() {
        val application: Application = ApplicationProvider.getApplicationContext()
        extractor = RumAnrAttributesExtractor(application)
    }

    @Test
    fun `flags the ANR with component anr and an error attribute`() {
        val builder = Attributes.builder()
        extractor.extract(builder, emptyArray())
        val attributes = builder.build()

        assertEquals(GlobalRumConstants.COMPONENT_ANR, attributes.get(GlobalRumConstants.COMPONENT_KEY))
        assertEquals("true", attributes.get(GlobalRumConstants.ERROR_KEY))
    }

    @Test
    fun `defaults app state to foreground when no lifecycle transition has been observed`() {
        // ANR detection is foreground-only, so a reported ANR with no observed transition
        // (e.g. late/hybrid init) is still attributed to the foreground.
        val builder = Attributes.builder()
        extractor.extract(builder, emptyArray())
        val attributes = builder.build()

        assertEquals(GlobalRumConstants.APP_STATE_FOREGROUND, attributes.get(GlobalRumConstants.APP_STATE_KEY))
    }
}
