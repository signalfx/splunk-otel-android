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

package com.splunk.rum.instrumentation.anr.internal.extractor

import android.app.Application
import android.content.Context
import com.splunk.rum.common.otel.internal.GlobalRumConstants
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.instrumentation.anr.AnrAttributesExtractor
import io.opentelemetry.api.common.AttributesBuilder

/**
 * Adds Splunk-specific attributes (component, error flag, and app state) to ANR events.
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
class RumAnrAttributesExtractor(context: Context) : AnrAttributesExtractor {

    private var appState: String? = null

    init {
        AppStateObserver.listeners += AppStateObserverListener()
        AppStateObserver.attach(context.applicationContext as Application)
    }

    override fun extract(attributes: AttributesBuilder, stackTrace: Array<StackTraceElement>) {
        attributes.put(GlobalRumConstants.COMPONENT_KEY, GlobalRumConstants.COMPONENT_ANR)
        attributes.put(GlobalRumConstants.ERROR_KEY, "true")
        val state = appState ?: GlobalRumConstants.APP_STATE_FOREGROUND
        attributes.put(GlobalRumConstants.APP_STATE_KEY, state)
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
