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
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RuntimeDetailsExtractorTest {

    @Test
    fun `adds storage and heap attributes`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val extractor = RuntimeDetailsExtractor.create(context)

        val builder = Attributes.builder()
        extractor.extract(builder, CrashDetails(Thread.currentThread(), RuntimeException("boom")))
        val attributes = builder.build()

        val storageFree = attributes.get(AttributeKey.longKey("storage.free"))
        val heapFree = attributes.get(AttributeKey.longKey("heap.free"))

        assertNotNull(storageFree)
        assertNotNull(heapFree)
        assertTrue(heapFree!! > 0)
    }
}
