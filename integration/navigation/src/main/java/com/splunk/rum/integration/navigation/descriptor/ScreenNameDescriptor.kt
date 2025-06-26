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
import com.splunk.rum.integration.navigation.screen.RumScreenName

internal object ScreenNameDescriptor {

    fun getName(activity: Activity): String = getName(activity as Any)

    fun isIgnored(activity: Activity): Boolean = isIgnored(activity as Any)

    fun getName(fragment: Fragment): String = getName(fragment as Any)

    fun isIgnored(fragment: Fragment): Boolean = isIgnored(fragment as Any)

    private fun getName(element: Any): String = getAnnotation(element)?.name ?: element::class.java.simpleName

    private fun isIgnored(element: Any): Boolean = getAnnotation(element)?.isIgnored ?: false

    private fun getAnnotation(element: Any): RumScreenName? {
        val javaClass = element::class.java
        return javaClass.getAnnotation(RumScreenName::class.java)
    }
}
