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

package com.splunk.rum.integration.agent.api

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SplunkRumTest {

    private val contextStorageProviderProperty = "io.opentelemetry.context.contextStorageProvider"

    @Before
    fun clearContextStorageProviderProperty() {
        System.clearProperty(contextStorageProviderProperty)
    }

    @Test
    fun `configureContextStorageProvider() sets property to default when unset`() {
        SplunkRum.configureContextStorageProvider()

        assertEquals("default", System.getProperty(contextStorageProviderProperty))
    }

    @Test
    fun `configureContextStorageProvider() leaves property alone when already set to default`() {
        System.setProperty(contextStorageProviderProperty, "default")

        SplunkRum.configureContextStorageProvider()

        assertEquals("default", System.getProperty(contextStorageProviderProperty))
    }

    @Test
    fun `configureContextStorageProvider() preserves a non-default custom provider configured by host app`() {
        val custom = "com.example.MyContextStorageProvider"
        System.setProperty(contextStorageProviderProperty, custom)

        SplunkRum.configureContextStorageProvider()

        assertEquals(custom, System.getProperty(contextStorageProviderProperty))
    }
}
