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

package com.splunk.rum.common.otel

import android.app.job.JobService
import com.splunk.rum.agent.common.otel.logRecord.UploadOtelLogRecordDataJob as CurrentUploadOtelLogRecordDataJob
import com.splunk.rum.agent.common.otel.logRecord.UploadSessionReplayDataJob as CurrentUploadSessionReplayDataJob
import com.splunk.rum.agent.common.otel.span.UploadOtelSpanDataJob as CurrentUploadOtelSpanDataJob
import com.splunk.rum.common.otel.logRecord.UploadOtelLogRecordDataJob
import com.splunk.rum.common.otel.logRecord.UploadSessionReplayDataJob
import com.splunk.rum.common.otel.span.UploadOtelSpanDataJob
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JobServiceCompatibilityTest {

    @Test
    fun `legacy upload services keep their old names`() {
        legacyServices.forEach { (legacyName, legacyClass, currentClass) ->
            assertEquals(legacyName, legacyClass.name)
            assertEquals(currentClass, legacyClass.superclass)
        }
    }

    @Test
    fun `legacy upload services remain instantiable JobService entry points`() {
        legacyServices.forEach { (_, serviceClass, _) ->
            assertTrue(JobService::class.java.isAssignableFrom(serviceClass))

            val constructor = serviceClass.getDeclaredConstructor()
            assertNotNull(constructor)
            assertTrue(Modifier.isPublic(constructor.modifiers))
            assertEquals(0, constructor.parameterCount)
        }
    }

    private companion object {
        val legacyServices: List<LegacyService> = listOf(
            LegacyService(
                "com.splunk.rum.common.otel.span.UploadOtelSpanDataJob",
                UploadOtelSpanDataJob::class.java,
                CurrentUploadOtelSpanDataJob::class.java
            ),
            LegacyService(
                "com.splunk.rum.common.otel.logRecord.UploadOtelLogRecordDataJob",
                UploadOtelLogRecordDataJob::class.java,
                CurrentUploadOtelLogRecordDataJob::class.java
            ),
            LegacyService(
                "com.splunk.rum.common.otel.logRecord.UploadSessionReplayDataJob",
                UploadSessionReplayDataJob::class.java,
                CurrentUploadSessionReplayDataJob::class.java
            )
        )

        private data class LegacyService(val legacyName: String, val legacyClass: Class<*>, val currentClass: Class<*>)
    }
}
