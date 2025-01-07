/*
 * Copyright 2024 Splunk Inc.
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

package com.smartlook.intergrationrun

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.fragment.app.Fragment
import com.smartlook.android.core.api.Smartlook
import com.smartlook.android.core.api.User
import com.smartlook.android.core.api.model.Properties
import com.smartlook.android.core.api.model.RecordingMask
import com.smartlook.sdk.log.LogListener
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.net.URL

@RunWith(RobolectricTestRunner::class)
internal class ApiTest {
    @Test
    fun testApi() {
        val smartlook = Smartlook.instance
        val preferences = smartlook.preferences

        val frameRate = preferences.frameRate
        preferences.frameRate = frameRate

        val navigation = preferences.eventTracking.navigation

        val disabledActivityClasses = navigation.disabledActivityClasses
        disabledActivityClasses.add(DummyActivity::class.java)

        val disabledFragmentClasses = navigation.disabledFragmentClasses
        disabledFragmentClasses.add(DummyFragment::class.java)

        val eventProperties = smartlook.eventProperties
        eventProperties.clear()
        eventProperties.getString("dummy")
        eventProperties.putString("dummy", "dummy")
        eventProperties.remove("Dummy")

        smartlook.log.allowedLogAspects = LogAspect.ALL
        smartlook.log.listeners += object : LogListener {
            override fun onLog(aspect: Long, severity: String?, tag: String, message: String) {
            }
        }

        smartlook.recordingMask = RecordingMask(emptyList())

        smartlook.trackEvent("a")

        smartlook.trackEvent(
            "a",
            Properties()
                .putString("a", "b")
                .putString("c", "d")
        )

        val userListeners = smartlook.user.listeners
        userListeners.add(object : User.Listener {
            override fun onUrlChanged(url: URL) {
            }
        })

        val sensitivity = smartlook.sensitivity
        val classSensitivity = sensitivity.getViewClassSensitivity(DummyView::class.java)
        sensitivity.setViewClassSensitivity(DummyView::class.java, classSensitivity)

        val view = DummyView(RuntimeEnvironment.getApplication())
        val viewSensitivity = sensitivity.getViewInstanceSensitivity(view)
        sensitivity.setViewInstanceSensitivity(view, viewSensitivity)

        val status = smartlook.state.status
    }

    private class DummyActivity : Activity()
    private class DummyFragment : Fragment()
    private class DummyView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr)
}
