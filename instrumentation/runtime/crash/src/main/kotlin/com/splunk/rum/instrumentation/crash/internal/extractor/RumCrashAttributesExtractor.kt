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

package com.splunk.rum.instrumentation.crash.internal.extractor

import android.app.Application
import android.content.Context
import com.splunk.rum.common.otel.internal.GlobalRumConstants
import com.splunk.rum.common.utils.AppStateObserver
import io.opentelemetry.api.common.AttributesBuilder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Adds Splunk-specific attributes (component, error flag, and app state) to crash events.
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
class RumCrashAttributesExtractor(context: Context) : CrashAttributesExtractor {

    private val crashHappened = AtomicBoolean(false)
    private var appState: String? = null

    init {
        AppStateObserver.listeners += AppStateObserverListener()
        AppStateObserver.attach(context.applicationContext as Application)
    }

    override fun extract(attributes: AttributesBuilder, crashDetails: CrashDetails) {
        // Set component=crash only for the first error that arrives here.
        // When multiple threads fail at roughly the same time (e.g. because of an OOM error),
        // the first error to arrive here is actually responsible for crashing the app; and all
        // the others that are captured before the OS actually kills the process are just additional
        // info (component=error).
        val component = if (crashHappened.compareAndSet(false, true)) {
            GlobalRumConstants.COMPONENT_CRASH
        } else {
            GlobalRumConstants.COMPONENT_ERROR
        }
        attributes.put(GlobalRumConstants.COMPONENT_KEY, component)
        attributes.put(GlobalRumConstants.ERROR_KEY, "true")
        appState?.let { attributes.put(GlobalRumConstants.APP_STATE_KEY, it) }
    }

    private inner class AppStateObserverListener : AppStateObserver.Listener {

        override fun onAppStarted() {
            appState = GlobalRumConstants.APP_STATE_CREATED
        }

        override fun onAppForegrounded() {
            appState = GlobalRumConstants.APP_STATE_FOREGROUND
        }

        override fun onAppBackgrounded() {
            appState = GlobalRumConstants.APP_STATE_BACKGROUND
        }

        override fun onAppClosed() {
            appState = GlobalRumConstants.APP_STATE_BACKGROUND
        }
    }
}
