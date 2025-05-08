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

package com.splunk.rum.integration.agent.api.subprocess

import android.app.Application
import android.os.Build
import com.cisco.android.common.utils.extensions.invoke
import com.cisco.android.common.utils.extensions.toKClass

internal object SubprocessDetector {
    fun isSubprocess(applicationId: String?): Boolean {
        val applicationProcessName = applicationProcessName
        // If application Id is same as application processName, the app is visible to user.
        // Using inverted condition to determine background processes.
        return applicationProcessName != applicationId
    }

    private val applicationProcessName: String
        get() = if (Build.VERSION.SDK_INT >= 28) {
            Application.getProcessName()
        } else try {
            val processName: String? = "android.app.ActivityThread".toKClass()
                ?.invoke<String>("currentProcessName")

            processName as String
        } catch (e: Exception) {
            ""
        }
}
