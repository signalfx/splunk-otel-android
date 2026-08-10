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

package com.splunk.rum.integration.navigation

import androidx.navigation.NavController
import com.splunk.rum.integration.navigation.automatic.NavigationEventEmitter
import com.splunk.rum.integration.navigation.automatic.ScreenChangeDetector
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationTest {

    private lateinit var navigation: Navigation
    private lateinit var detector: ScreenChangeDetector
    private lateinit var navController: NavController

    @Before
    fun setUp() {
        navigation = Navigation()
        detector = ScreenChangeDetector(NavigationEventEmitter())
        navController = mock(NavController::class.java)
    }

    @Test
    fun `registerNavController without install is a no-op`() {
        navigation.registerNavController(navController)

        assertNull(navigation.composeTracker)
    }

    @Test
    fun `registerNavController creates tracker when config is pending`() {
        navigation.setTrackerConfig(detector, null)

        navigation.registerNavController(navController)

        assertNotNull(navigation.composeTracker)
    }

    @Test
    fun `registerNavController reuses tracker on second call`() {
        navigation.setTrackerConfig(detector, null)

        navigation.registerNavController(navController)
        val firstTracker = navigation.composeTracker

        val secondController = mock(NavController::class.java)
        navigation.registerNavController(secondController)

        assertSame(firstTracker, navigation.composeTracker)
    }

    @Test
    fun `clearTrackerConfig prevents tracker creation`() {
        navigation.setTrackerConfig(detector, null)
        navigation.clearTrackerConfig()

        navigation.registerNavController(navController)

        assertNull(navigation.composeTracker)
    }
}
