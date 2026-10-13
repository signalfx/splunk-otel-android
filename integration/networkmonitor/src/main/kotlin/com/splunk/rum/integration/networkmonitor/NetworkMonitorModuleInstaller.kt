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

package com.splunk.rum.integration.networkmonitor

import android.content.Context
import android.util.Log
import com.splunk.rum.integration.agent.internal.module.ModuleInstaller

internal class NetworkMonitorModuleInstaller : ModuleInstaller() {

    override fun onInstall(context: Context) {
        try {
            NetworkMonitorModuleIntegration.attach(context)
        } catch (exception: Exception) {
            // A failed optional module must not abort ContentProvider startup or the host app.
            Log.w(TAG, "Failed to attach network monitoring; continuing without it.", exception)
        }
    }

    private companion object {
        private const val TAG = "NetworkMonitorInstaller"
    }
}
