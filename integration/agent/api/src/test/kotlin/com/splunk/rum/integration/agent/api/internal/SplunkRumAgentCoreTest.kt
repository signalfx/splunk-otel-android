/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.integration.agent.api.internal

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.splunk.android.common.storage.extensions.noBackupFilesDirCompat
import com.splunk.rum.common.storage.AgentStorage
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.EndpointConfiguration
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import com.splunk.rum.integration.agent.internal.user.IUserManager
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class SplunkRumAgentCoreTest {

    private lateinit var application: Application
    private lateinit var storage: AgentStorage
    private lateinit var testStorageDir: File
    private lateinit var mockUserManager: IUserManager
    private lateinit var mockSessionManager: ISplunkSessionManager
    private lateinit var mockAgentConfig: AgentConfiguration

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()

        storage = AgentStorage.attach(application) as AgentStorage

        testStorageDir = File(application.noBackupFilesDirCompat, "agent")

        mockUserManager = mock(IUserManager::class.java)
        mockSessionManager = mock(ISplunkSessionManager::class.java)
        mockAgentConfig = mock(AgentConfiguration::class.java, RETURNS_DEEP_STUBS)

        // Setup some needed defaults
        `when`(mockAgentConfig.session.samplingRate).thenReturn(1.0)
        `when`(mockAgentConfig.copy()).thenReturn(mockAgentConfig)
        `when`(mockAgentConfig.endpoint).thenReturn(
            EndpointConfiguration("test", "test-token")
        )
    }

    @After
    fun tearDown() {
        cleanupStorage()
        SplunkRumAgentCore.isRunning = false
        AgentIntegration.modules.clear()
    }

    @Test
    fun `install generates app installation id on first run`() {
        val initialId = storage.readAppInstallationId()

        assertNull("Initial installation ID should be null", initialId)

        installSplunkRumAgent()

        val storedId = storage.readAppInstallationId()
        assertNotNull("Installation ID should be generated", storedId)
    }

    @Test
    fun `install persists app installation id across multiple calls`() {
        // First install
        installSplunkRumAgent()
        val firstCallId = storage.readAppInstallationId()

        // Reset running state to allow re-installation
        SplunkRumAgentCore.isRunning = false
        AgentIntegration.modules.clear()

        // Second install (simulating app restart)
        installSplunkRumAgent()
        val secondCallId = storage.readAppInstallationId()

        assertEquals("Installation ID should persist across calls", firstCallId, secondCallId)
    }

    @Test
    fun `install generates unique ids for fresh installations`() {
        // First installation
        installSplunkRumAgent()
        val firstId = storage.readAppInstallationId()

        // Simulate fresh install
        cleanupStorage()
        SplunkRumAgentCore.isRunning = false
        AgentIntegration.modules.clear()
        storage = AgentStorage.attach(application) as AgentStorage

        // Second installation
        installSplunkRumAgent()
        val secondId = storage.readAppInstallationId()

        assertNotNull("First ID should exist", firstId)
        assertNotNull("Second ID should exist", secondId)
        assertNotEquals("Installation IDs should be unique", firstId, secondId)
    }

    private fun installSplunkRumAgent() {
        SplunkRumAgentCore.install(
            application,
            mockAgentConfig,
            mockUserManager,
            mockSessionManager,
            emptyList(),
            MutableAttributes()
        )
    }

    private fun cleanupStorage() {
        if (testStorageDir.exists()) {
            testStorageDir.deleteRecursively()
        }

        resetAgentStorageSingleton()
    }

    private fun resetAgentStorageSingleton() {
        try {
            val field = AgentStorage::class.java.getDeclaredField("instance")
            field.isAccessible = true
            field.set(null, null)
        } catch (e: Exception) {
            // Ignore reflection errors in tests
        }
    }
}
