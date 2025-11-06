package com.splunk.rum.integration.agent.api.resource

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.splunk.rum.integration.agent.api.AgentConfiguration
import io.opentelemetry.api.common.AttributeKey
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class AgentResourceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `allResource contains app installation id attribute`() {
        val testInstallationId = "550e8400-e29b-41d4-a716-446655440000"
        val mockAgentConfig = mock(AgentConfiguration::class.java)

        val resource = AgentResource.allResource(context, testInstallationId, mockAgentConfig)

        val actualId = resource.getAttribute(AttributeKey.stringKey("app.installation.id"))
        assertEquals(testInstallationId, actualId)
    }
}
