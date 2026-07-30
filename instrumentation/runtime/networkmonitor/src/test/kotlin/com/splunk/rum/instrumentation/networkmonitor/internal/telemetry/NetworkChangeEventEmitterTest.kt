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

import com.splunk.rum.instrumentation.networkmonitor.internal.lifecycle.NetworkApplicationStateGate
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder
import io.opentelemetry.api.logs.Logger
import org.junit.Test
import org.mockito.Answers.RETURNS_SELF
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class NetworkChangeEventEmitterTest {
    private val logger = mock(Logger::class.java)
    private val logRecordBuilder = mock(ExtendedLogRecordBuilder::class.java, RETURNS_SELF)
    private val gate = NetworkApplicationStateGate()
    private val emitter = NetworkChangeEventEmitter(logger, gate)
    private val attributes = Attributes.of(AttributeKey.stringKey("network.connection.type"), "wifi")

    init {
        `when`(logger.logRecordBuilder()).thenReturn(logRecordBuilder)
    }

    @Test
    fun emitsNamedEventWithAttributesInForeground() {
        emitter.emit(attributes)

        verify(logRecordBuilder).setEventName(NetworkChangeEventEmitter.EVENT_NAME)
        verify(logRecordBuilder).setAllAttributes(attributes)
        verify(logRecordBuilder).emit()
    }

    @Test
    fun doesNotBuildEventInBackground() {
        gate.onAppBackgrounded()

        emitter.emit(attributes)

        verify(logger, never()).logRecordBuilder()
    }

    @Test
    fun resumesEmissionAfterReturningToForeground() {
        gate.onAppBackgrounded()
        emitter.emit(attributes)
        gate.onAppForegrounded()

        emitter.emit(attributes)

        verify(logRecordBuilder).emit()
    }
}
