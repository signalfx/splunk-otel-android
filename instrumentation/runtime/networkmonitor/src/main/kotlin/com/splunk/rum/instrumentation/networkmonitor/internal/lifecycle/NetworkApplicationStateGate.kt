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

package com.splunk.rum.instrumentation.networkmonitor.internal.lifecycle

import com.splunk.rum.common.utils.AppStateObserver
import java.util.concurrent.atomic.AtomicBoolean

internal class NetworkApplicationStateGate : AppStateObserver.Listener {
    private val foreground = AtomicBoolean(true)

    val canEmit: Boolean
        get() = foreground.get()

    override fun onAppStarted() {
        foreground.set(true)
    }

    override fun onAppForegrounded() {
        foreground.set(true)
    }

    override fun onAppBackgrounded() {
        foreground.set(false)
    }

    override fun onAppClosed() {
        foreground.set(false)
    }
}
