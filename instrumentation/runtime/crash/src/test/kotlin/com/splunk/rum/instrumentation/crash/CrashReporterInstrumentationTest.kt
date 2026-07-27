/*
 * Copyright 2026 Splunk Inc.
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.crash

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.opentelemetry.api.OpenTelemetry
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashReporterInstrumentationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var originalHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setUp() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        CrashReporterInstrumentation.installed.set(false)
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        CrashReporterInstrumentation.installed.set(false)
    }

    @Test
    fun `install is idempotent and does not re-wrap the default handler`() {
        val instrumentation = CrashReporterInstrumentation()

        instrumentation.install(context, OpenTelemetry.noop())
        val afterFirst = Thread.getDefaultUncaughtExceptionHandler()

        instrumentation.install(context, OpenTelemetry.noop())
        val afterSecond = Thread.getDefaultUncaughtExceptionHandler()

        assertTrue(afterFirst is CrashReportingExceptionHandler)
        assertSame(afterFirst, afterSecond)
    }

    @Test
    fun `install is idempotent across separate instances`() {
        CrashReporterInstrumentation().install(context, OpenTelemetry.noop())
        val afterFirst = Thread.getDefaultUncaughtExceptionHandler()

        // A second instance (e.g. from reconfiguration) must not chain a handler or receiver.
        CrashReporterInstrumentation().install(context, OpenTelemetry.noop())
        val afterSecond = Thread.getDefaultUncaughtExceptionHandler()

        assertTrue(afterFirst is CrashReportingExceptionHandler)
        assertSame(afterFirst, afterSecond)
    }
}
