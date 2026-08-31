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

package com.splunk.rum.integration.applicationlifecycle

import com.splunk.rum.common.utils.AppStateObserver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApplicationLifecycleModuleIntegrationTest {

    private val moduleListener: AppStateObserver.Listener
        get() = ApplicationLifecycleModuleIntegration.appStateListenerReference

    @Before
    fun setUp() {
        AppStateObserver.listeners.clear()
    }

    @After
    fun tearDown() {
        AppStateObserver.listeners.clear()
    }

    @Test
    fun disableRemovesModuleListenerFromAppStateObserver() {
        AppStateObserver.listeners += moduleListener
        assertTrue(AppStateObserver.listeners.contains(moduleListener))

        ApplicationLifecycleModuleIntegration.disableAndRemoveListener()

        assertFalse(
            "Module listener should be removed when module is disabled",
            AppStateObserver.listeners.contains(moduleListener)
        )
    }

    @Test
    fun disableIsIdempotent() {
        AppStateObserver.listeners += moduleListener

        ApplicationLifecycleModuleIntegration.disableAndRemoveListener()
        ApplicationLifecycleModuleIntegration.disableAndRemoveListener()

        assertFalse(
            "Repeated disable should not throw or re-add the listener",
            AppStateObserver.listeners.contains(moduleListener)
        )
    }

    @Test
    fun disableDoesNotAffectOtherModuleListeners() {
        val sessionManagerListener = object : AppStateObserver.Listener {}
        val crashListener = object : AppStateObserver.Listener {}

        AppStateObserver.listeners += sessionManagerListener
        AppStateObserver.listeners += moduleListener
        AppStateObserver.listeners += crashListener

        ApplicationLifecycleModuleIntegration.disableAndRemoveListener()

        assertFalse(AppStateObserver.listeners.contains(moduleListener))
        assertTrue(
            "Session manager listener should remain",
            AppStateObserver.listeners.contains(sessionManagerListener)
        )
        assertTrue(
            "Crash listener should remain",
            AppStateObserver.listeners.contains(crashListener)
        )
        assertEquals(2, AppStateObserver.listeners.size)
    }

    @Test
    fun disableRemovesOnlyOneInstanceEvenIfDuplicatelyRegistered() {
        AppStateObserver.listeners += moduleListener
        AppStateObserver.listeners += moduleListener

        ApplicationLifecycleModuleIntegration.disableAndRemoveListener()

        assertEquals(
            "Only one instance should be removed per disable call (MutableList -= semantics)",
            1,
            AppStateObserver.listeners.count { it === moduleListener }
        )
    }
}
