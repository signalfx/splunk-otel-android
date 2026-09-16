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

package com.splunk.rum.integration.agent.api.resource

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.splunk.rum.integration.agent.api.AgentConfiguration
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.resources.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class AgentResourceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `allResource contains app installation id attribute`() {
        val testInstallationId = "550e8400e29b41d4a716446655440000"
        val mockAgentConfig = mock(AgentConfiguration::class.java)

        val resource = AgentResource.allResource(context, testInstallationId, mockAgentConfig)

        val actualId = resource.getAttribute(AttributeKey.stringKey("app.installation.id"))
        assertEquals(testInstallationId, actualId)
    }

    @Test
    fun `allResource emits deployment environment under semconv deployment_environment_name key`() {
        val testInstallationId = "550e8400e29b41d4a716446655440000"
        val testEnvironment = "production"
        val mockAgentConfig = mock(AgentConfiguration::class.java)
        `when`(mockAgentConfig.deploymentEnvironment).thenReturn(testEnvironment)

        val resource = AgentResource.allResource(context, testInstallationId, mockAgentConfig)

        assertEquals(
            testEnvironment,
            resource.getAttribute(AttributeKey.stringKey("deployment.environment.name"))
        )
        assertNull(resource.getAttribute(AttributeKey.stringKey("deployment.environment")))
    }

    @Test
    fun `allResource includes default OTel SDK resource attributes`() {
        val mockAgentConfig = mock(AgentConfiguration::class.java)

        val resource = AgentResource.allResource(context, "test-id", mockAgentConfig)

        assertNotNull(resource.getAttribute(AttributeKey.stringKey("telemetry.sdk.name")))
        assertNotNull(resource.getAttribute(AttributeKey.stringKey("telemetry.sdk.language")))
        assertNotNull(resource.getAttribute(AttributeKey.stringKey("telemetry.sdk.version")))
    }

    @Test
    fun `allResource merged with empty produces same result as merged with getDefault`() {
        val mockAgentConfig = mock(AgentConfiguration::class.java)
        `when`(mockAgentConfig.appName).thenReturn("test-app")
        `when`(mockAgentConfig.deploymentEnvironment).thenReturn("test")

        val allResource = AgentResource.allResource(context, "test-id", mockAgentConfig)

        val fromEmpty = Resource.empty().merge(allResource)
        val fromDefault = Resource.getDefault().merge(allResource)

        assertEquals(fromDefault.attributes, fromEmpty.attributes)
    }
}
