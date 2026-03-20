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

package com.splunk.rum.integration.navigation.descriptor

import android.app.Activity
import androidx.fragment.app.Fragment
import com.splunk.rum.integration.navigation.NavigationElement
import com.splunk.rum.integration.navigation.screen.RumScreenName

internal object ScreenNameDescriptor {

    fun getName(activity: Activity): String = getName(activity as Any)

    fun isIgnored(activity: Activity): Boolean = isIgnored(activity as Any)

    fun getName(fragment: Fragment): String = getName(fragment as Any)

    fun isIgnored(fragment: Fragment): Boolean = isIgnored(fragment as Any)

    private fun getName(element: Any): String =
        getNavigationElement(element)?.name ?: getRumScreenName(element)?.name ?: element::class.java.simpleName

    private fun isIgnored(element: Any): Boolean {
        // Ignore NavHostFragment by default (Jetpack Navigation container; not a user-facing screen).
        if (element is Fragment && element.javaClass.name == NAV_HOST_FRAGMENT_CLASS_NAME) {
            return true
        }
        return getNavigationElement(element)?.isIgnored ?: getRumScreenName(element)?.isIgnored ?: false
    }

    private const val NAV_HOST_FRAGMENT_CLASS_NAME = "androidx.navigation.fragment.NavHostFragment"

    private fun getRumScreenName(element: Any): RumScreenName? {
        val javaClass = element::class.java
        return javaClass.getAnnotation(RumScreenName::class.java)
    }

    private fun getNavigationElement(element: Any): NavigationElement? {
        val javaClass = element::class.java
        return javaClass.getAnnotation(NavigationElement::class.java)
    }
}
