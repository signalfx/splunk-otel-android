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

package com.splunk.rum.integration.agent.api

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.splunk.rum.agent.common.otel.OpenTelemetryInitializer
import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 22])
class OtelApiCompatibilitySmokeTest {

    @After
    fun tearDown() {
        SplunkOpenTelemetrySdk.shutdown()
    }

    @Test
    fun `initializes OpenTelemetry and creates a span`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val openTelemetry = OpenTelemetryInitializer(
            application = application,
            deferredUntilForeground = true
        ).build()

        val span = openTelemetry.getTracer("api-compatibility-smoke")
            .spanBuilder("smoke")
            .startSpan()

        assertTrue(span.spanContext.isValid)
        span.end()
    }
}
