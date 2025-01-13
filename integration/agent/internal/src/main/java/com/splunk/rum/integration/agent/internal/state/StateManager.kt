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

package com.splunk.rum.integration.agent.internal.state

import android.app.Application
import android.content.Context
import com.splunk.sdk.common.utils.AppStateObserver

class StateManager(context: Context) {

    var state = State.CREATED

    private val appStateObserver = AppStateObserver()

    init {
        appStateObserver.listener = AppStateObserverListener()
        appStateObserver.attach(context.applicationContext as Application)
    }

    private inner class AppStateObserverListener : AppStateObserver.Listener {

        override fun onAppStarted() {
            state = State.CREATED
        }

        override fun onAppBackgrounded() {
            state = State.BACKGROUND
        }

        override fun onAppForegrounded() {
            state = State.FOREGROUND
        }

        override fun onAppClosed() {
        }
    }

    enum class State(val value: String) {
        CREATED("created"), FOREGROUND("foreground"), BACKGROUND("background")
    }

    companion object {
        private const val TAG = "StateManager"
        private var instanceInternal: StateManager? = null
        fun obtainInstance(context: Context): StateManager {
            if (instanceInternal == null) instanceInternal = StateManager(context)

            return requireNotNull(instanceInternal)
        }
    }
}
