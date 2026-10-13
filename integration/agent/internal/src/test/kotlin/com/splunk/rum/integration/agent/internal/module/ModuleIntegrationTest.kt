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

package com.splunk.rum.integration.agent.internal.module

import android.app.Application
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager
import io.opentelemetry.api.OpenTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ModuleIntegrationTest {

    @Before
    fun setUp() {
        val instanceField = AgentIntegration::class.java.getDeclaredField("instanceInternal")
        instanceField.isAccessible = true
        instanceField.set(null, null)
        AgentIntegration.modules.clear()
    }

    @Test
    fun `attach defers session manager and listener wiring until manager is ready`() {
        val application = RuntimeEnvironment.getApplication() as Application
        val module = TestModuleIntegration()

        module.attach(application)

        val sessionListeners = mutableSetOf<SplunkSessionManager.SessionListener>()
        val sessionManager = mock(ISplunkSessionManager::class.java)
        `when`(sessionManager.sessionListeners).thenReturn(sessionListeners)

        val agentIntegration = AgentIntegration.obtainInstance(application)
        agentIntegration.listeners.single().onSessionManagerReady(sessionManager)
        sessionListeners.single().onSessionChanged("session-id", 1L)

        assertSame(sessionManager, module.attachedSessionManager())
        assertEquals("session-id", module.lastSessionId)
    }

    @Test
    fun `module does not register a session listener by default`() {
        val application = RuntimeEnvironment.getApplication() as Application
        val module = DefaultModuleIntegration()
        module.attach(application)

        val sessionListeners = mutableSetOf<SplunkSessionManager.SessionListener>()
        val sessionManager = mock(ISplunkSessionManager::class.java)
        `when`(sessionManager.sessionListeners).thenReturn(sessionListeners)

        AgentIntegration.obtainInstance(application).listeners.single().onSessionManagerReady(sessionManager)

        assertSame(sessionManager, module.attachedSessionManager())
        assertEquals(0, sessionListeners.size)
    }

    private class TestModuleIntegration : ModuleIntegration<TestModuleConfiguration>(TestModuleConfiguration) {
        var lastSessionId: String? = null

        override val observesSessionChanges: Boolean = true

        fun attachedSessionManager(): ISplunkSessionManager = sessionManager

        override fun onInstall(
            application: Application,
            openTelemetry: OpenTelemetry,
            moduleConfigurations: List<ModuleConfiguration>
        ) = Unit

        override fun onSessionChange(sessionId: String) {
            lastSessionId = sessionId
        }
    }

    private class DefaultModuleIntegration : ModuleIntegration<TestModuleConfiguration>(TestModuleConfiguration) {
        fun attachedSessionManager(): ISplunkSessionManager = sessionManager

        override fun onInstall(
            application: Application,
            openTelemetry: OpenTelemetry,
            moduleConfigurations: List<ModuleConfiguration>
        ) = Unit
    }

    private object TestModuleConfiguration : ModuleConfiguration {
        override val name: String = "test"
        override val attributes: List<Pair<String, String>> = emptyList()
    }
}
