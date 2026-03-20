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

package com.splunk.rum.integration.navigation.descriptor

import android.app.Activity
import androidx.fragment.app.Fragment
import com.splunk.rum.integration.navigation.NavigationElement
import com.splunk.rum.integration.navigation.screen.RumScreenName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenNameDescriptorTest {

    @Test
    fun `getName returns NavigationElement name when present`() {
        val fragment = MenuFragment()
        assertEquals("Menu", ScreenNameDescriptor.getName(fragment))
    }

    @Test
    fun `getName returns RumScreenName when NavigationElement is absent`() {
        val fragment = CrashReportsFragment()
        assertEquals("Crash Reports", ScreenNameDescriptor.getName(fragment))
    }

    @Test
    fun `getName returns class simpleName when no annotations present`() {
        val fragment = SlowRenderingFragment()
        assertEquals("SlowRenderingFragment", ScreenNameDescriptor.getName(fragment))
    }

    @Test
    fun `getName prefers NavigationElement over RumScreenName when both present`() {
        val fragment = OkHttpFragment()
        assertEquals("OkHttp", ScreenNameDescriptor.getName(fragment))
    }

    @Test
    fun `getName works for Activity with NavigationElement`() {
        val activity = MainActivity()
        assertEquals("Main", ScreenNameDescriptor.getName(activity))
    }

    @Test
    fun `getName works for Activity with RumScreenName`() {
        val activity = SettingsActivity()
        assertEquals("Settings", ScreenNameDescriptor.getName(activity))
    }

    @Test
    fun `getName returns class simpleName for plain Activity`() {
        val activity = OnboardingActivity()
        assertEquals("OnboardingActivity", ScreenNameDescriptor.getName(activity))
    }

    @Test
    fun `isIgnored returns false for unannotated fragment`() {
        val fragment = SlowRenderingFragment()
        assertFalse(ScreenNameDescriptor.isIgnored(fragment))
    }

    @Test
    fun `isIgnored returns true when NavigationElement isIgnored is true`() {
        val fragment = WebViewContainerFragment()
        assertTrue(ScreenNameDescriptor.isIgnored(fragment))
    }

    @Test
    fun `isIgnored returns true when RumScreenName isIgnored is true`() {
        val fragment = EndpointConfigurationFragment()
        assertTrue(ScreenNameDescriptor.isIgnored(fragment))
    }

    @Test
    fun `isIgnored returns false for unannotated activity`() {
        val activity = OnboardingActivity()
        assertFalse(ScreenNameDescriptor.isIgnored(activity))
    }

    @Test
    fun `isIgnored returns true when activity NavigationElement isIgnored is true`() {
        val activity = SplashActivity()
        assertTrue(ScreenNameDescriptor.isIgnored(activity))
    }

    @NavigationElement(name = "Menu")
    class MenuFragment : Fragment()

    @RumScreenName(name = "Crash Reports")
    class CrashReportsFragment : Fragment()

    class SlowRenderingFragment : Fragment()

    @NavigationElement(name = "OkHttp")
    @RumScreenName(name = "OkHttp Network")
    class OkHttpFragment : Fragment()

    @NavigationElement(name = "WebView", isIgnored = true)
    class WebViewContainerFragment : Fragment()

    @RumScreenName(name = "Endpoint Configuration", isIgnored = true)
    class EndpointConfigurationFragment : Fragment()

    @NavigationElement(name = "Main")
    class MainActivity : Activity()

    @RumScreenName(name = "Settings")
    class SettingsActivity : Activity()

    class OnboardingActivity : Activity()

    @NavigationElement(name = "Splash", isIgnored = true)
    class SplashActivity : Activity()
}
