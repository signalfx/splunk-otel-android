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
