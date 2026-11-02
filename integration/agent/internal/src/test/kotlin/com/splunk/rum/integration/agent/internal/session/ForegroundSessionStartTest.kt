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

package com.splunk.rum.integration.agent.internal.session

import android.app.Activity
import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.integration.agent.internal.AgentIntegration
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Logger
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers.RETURNS_SELF
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ForegroundSessionStartTest {

    @After
    fun cleanUpObserver() {
        AppStateObserver.detach()
        AppStateObserver.listeners.clear()
    }

    @Test
    fun `foreground-aware session start is emitted when the Activity starts`() {
        val application = RuntimeEnvironment.getApplication()
        val builder = mock(LogRecordBuilder::class.java, RETURNS_SELF)
        val logger = mock(Logger::class.java)
        `when`(logger.logRecordBuilder()).thenReturn(builder)
        val integration = AgentIntegration.obtainInstance(application)
        integration.sessionStartEmitter.attach(logger)
        val storage = mock(IAgentStorage::class.java)
        `when`(storage.readSessionIds()).thenReturn(emptyList())
        val manager = integration.createSessionManager(storage, includeBackgroundOnlySessions = false)

        manager.reset()
        manager.install(application)
        assertNull(manager.sessionId)
        verify(builder, never()).emit()

        val activity = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        verify(builder, times(1)).emit()
        activity.pause().stop().destroy()
    }

    @Test
    fun `compatibility mode still holds session start until telemetry is accepted`() {
        val application = RuntimeEnvironment.getApplication()
        val builder = mock(LogRecordBuilder::class.java, RETURNS_SELF)
        val logger = mock(Logger::class.java)
        `when`(logger.logRecordBuilder()).thenReturn(builder)
        val integration = AgentIntegration.obtainInstance(application)
        integration.sessionStartEmitter.attach(logger)
        val storage = mock(IAgentStorage::class.java)
        `when`(storage.readSessionIds()).thenReturn(emptyList())
        val manager = integration.createSessionManager(storage, includeBackgroundOnlySessions = true)

        manager.reset()
        manager.install(application)
        verify(builder, never()).emit()

        integration.emitSessionStartIfPending()
        verify(builder, times(1)).emit()
    }
}
