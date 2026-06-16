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
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.InstrumentationScopeInfo
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.sdk.logs.data.LogRecordData
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ScreenNameLogRecordProcessorTest {

    private val tracker = object : IScreenNameTracker {
        override var lastScreenName: String? = null
        override var screenName: String = "HomeFragment"
    }

    private val processor = ScreenNameLogRecordProcessor(tracker)

    @Test
    fun `stamps screen name on log record that has none`() {
        val logRecord = mockLogRecord(screenName = null)

        processor.onEmit(Context.root(), logRecord)

        verify(logRecord).setAttribute(GlobalRumConstants.SCREEN_NAME_KEY, "HomeFragment")
    }

    @Test
    fun `does not overwrite screen name already present on log record`() {
        val logRecord = mockLogRecord(screenName = "OkHttpFragment")

        processor.onEmit(Context.root(), logRecord)

        verify(logRecord, never()).setAttribute(GlobalRumConstants.SCREEN_NAME_KEY, "HomeFragment")
    }

    @Test
    fun `snapshots current tracker value at emit time`() {
        tracker.screenName = "SettingsFragment"
        val logRecord = mockLogRecord(screenName = null)

        processor.onEmit(Context.root(), logRecord)

        verify(logRecord).setAttribute(GlobalRumConstants.SCREEN_NAME_KEY, "SettingsFragment")
    }

    private fun mockLogRecord(screenName: String?): ReadWriteLogRecord {
        val logRecord = mock(ReadWriteLogRecord::class.java)
        val logRecordData = mock(LogRecordData::class.java)
        val attributes = if (screenName != null) {
            Attributes.of(GlobalRumConstants.SCREEN_NAME_KEY, screenName)
        } else {
            Attributes.empty()
        }
        `when`(logRecord.toLogRecordData()).thenReturn(logRecordData)
        `when`(logRecordData.attributes).thenReturn(attributes)
        `when`(logRecordData.instrumentationScopeInfo).thenReturn(InstrumentationScopeInfo.empty())
        return logRecord
    }
}
