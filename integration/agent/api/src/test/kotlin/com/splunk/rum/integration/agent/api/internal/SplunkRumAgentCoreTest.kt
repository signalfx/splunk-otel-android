package com.splunk.rum.integration.agent.api.internal

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.splunk.android.common.storage.extensions.noBackupFilesDirCompat
import com.splunk.rum.common.storage.AgentStorage
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import com.splunk.rum.integration.agent.internal.user.IUserManager
import java.io.File
import java.net.URL
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
        `when`(mockAgentConfig.session).thenReturn(mock())
        `when`(mockAgentConfig.session.samplingRate).thenReturn(1.0)
        `when`(mockAgentConfig.copy()).thenReturn(mockAgentConfig)
        `when`(mockAgentConfig.endpoint).thenReturn(mock())
        `when`(mockAgentConfig.endpoint.sessionReplayEndpoint).thenReturn(URL("https://example.com/replay"))
        `when`(mockAgentConfig.endpoint.traceEndpoint).thenReturn(URL("https://example.com/trace"))
    }

    @After
    fun tearDown() {
        cleanupStorage()
        SplunkRumAgentCore.isRunning = false
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
            emptyList()
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
