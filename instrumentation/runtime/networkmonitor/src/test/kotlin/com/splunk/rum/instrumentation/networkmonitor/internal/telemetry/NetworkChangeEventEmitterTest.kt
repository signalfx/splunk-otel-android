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

package com.splunk.rum.instrumentation.networkmonitor.internal.telemetry

import com.splunk.rum.common.logger.Logger as SdkLogger
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Logger
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers.RETURNS_SELF
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkChangeEventEmitterTest {
    private val logger = mock(Logger::class.java)
    private val logRecordBuilder = mock(LogRecordBuilder::class.java, RETURNS_SELF)
    private val emitter = NetworkChangeEventEmitter(logger)
    private val attributes = Attributes.of(AttributeKey.stringKey("network.connection.type"), "wifi")

    init {
        SdkLogger.clearLogs()
        `when`(logger.logRecordBuilder()).thenReturn(logRecordBuilder)
    }

    @Test
    fun emitsNamedEventWithAttributesInForeground() {
        emitter.emit(attributes, isAppForegrounded = true, networkChangeTimestampMillis = 1234L)

        verify(logRecordBuilder).setAllAttributes(attributes)
        verify(logRecordBuilder).setAttribute(
            NetworkChangeEventEmitter.EVENT_NAME_KEY,
            NetworkChangeEventEmitter.EVENT_NAME
        )
        verify(logRecordBuilder).setTimestamp(1234L, TimeUnit.MILLISECONDS)
        verify(logRecordBuilder).setObservedTimestamp(anyLong(), eq(TimeUnit.MILLISECONDS))
        verify(logRecordBuilder).emit()
    }

    @Test
    fun doesNotBuildEventInBackground() {
        emitter.emit(attributes, isAppForegrounded = false, networkChangeTimestampMillis = 1234L)

        verify(logger, never()).logRecordBuilder()
    }

    @Test
    fun resumesEmissionAfterReturningToForeground() {
        emitter.emit(attributes, isAppForegrounded = false, networkChangeTimestampMillis = 1234L)

        emitter.emit(attributes, isAppForegrounded = true, networkChangeTimestampMillis = 1234L)

        verify(logRecordBuilder).emit()
    }

    @Test
    fun loggerFailureDoesNotEscapeNetworkChangeEmission() {
        `when`(logger.logRecordBuilder()).thenThrow(IllegalStateException("logger unavailable"))

        emitter.emit(attributes, isAppForegrounded = true, networkChangeTimestampMillis = 1234L)

        assertTrue(
            SdkLogger.logs.any {
                it.tag == "NetworkChangeEmitter" && it.message == "Failed to emit network change event."
            }
        )
    }
}
