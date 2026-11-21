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

import com.splunk.rum.integration.applicationlifecycle.model.AppState
import com.splunk.rum.integration.applicationlifecycle.model.ApplicationLifecycleData

/** Keeps timestamps for one Activity attempt until a foreground session is available. */
internal class StartupLifecycleContext {
    private var created: ApplicationLifecycleData? = null
    private var foreground: ApplicationLifecycleData? = null

    @Synchronized
    fun hold(event: ApplicationLifecycleData) {
        when (event.appState) {
            AppState.CREATED -> created = event
            AppState.FOREGROUND -> foreground = event
            AppState.BACKGROUND -> clear()
        }
    }

    @Synchronized
    fun take(): List<ApplicationLifecycleData> = listOfNotNull(created, foreground).also { clear() }

    @Synchronized
    fun clear() {
        created = null
        foreground = null
    }
}
