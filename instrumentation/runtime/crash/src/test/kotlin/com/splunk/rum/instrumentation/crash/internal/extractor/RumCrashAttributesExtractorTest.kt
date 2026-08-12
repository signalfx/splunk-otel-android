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

package com.splunk.rum.instrumentation.crash.internal.extractor

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
class RumCrashAttributesExtractorTest {

    private lateinit var extractor: RumCrashAttributesExtractor

    @Before
    fun setUp() {
        val application: Application = ApplicationProvider.getApplicationContext()
        extractor = RumCrashAttributesExtractor(application)
    }

    @Test
    fun `marks the first crash as component crash and flags it as an error`() {
        val attributes = extract()

        assertEquals(GlobalRumConstants.COMPONENT_CRASH, attributes.get(GlobalRumConstants.COMPONENT_KEY))
        assertEquals("true", attributes.get(GlobalRumConstants.ERROR_KEY))
    }

    @Test
    fun `marks subsequent concurrent crashes as component error`() {
        val first = extract()
        val second = extract()

        assertEquals(GlobalRumConstants.COMPONENT_CRASH, first.get(GlobalRumConstants.COMPONENT_KEY))
        assertEquals(GlobalRumConstants.COMPONENT_ERROR, second.get(GlobalRumConstants.COMPONENT_KEY))
    }

    private fun extract(): Attributes {
        val builder = Attributes.builder()
        extractor.extract(builder, CrashDetails(Thread.currentThread(), RuntimeException("boom")))
        return builder.build()
    }
}
