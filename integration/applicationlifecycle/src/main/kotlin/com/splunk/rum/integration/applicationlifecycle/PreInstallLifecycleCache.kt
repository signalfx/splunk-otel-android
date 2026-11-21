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

import com.splunk.rum.integration.applicationlifecycle.model.ApplicationLifecycleData

/** Keeps pre-install events for background coverage and the latest foreground attempt. */
internal class PreInstallLifecycleCache {

    private val events = mutableListOf<ApplicationLifecycleData>()
    private val foregroundEvents = StartupLifecycleContext()

    fun add(event: ApplicationLifecycleData) {
        foregroundEvents.hold(event)
        if (events.size < MAX_CACHE_SIZE) events += event
    }

    fun markAppClosed() {
        foregroundEvents.clear()
    }

    /** Returns the pending CREATED and FOREGROUND events even if the ten event cache is full. */
    fun eventsToReport(allowBackgroundSessionCreation: Boolean): List<ApplicationLifecycleData> =
        if (allowBackgroundSessionCreation) events.toList() else foregroundEvents.take()

    fun clear() {
        events.clear()
        foregroundEvents.clear()
    }

    private companion object {
        const val MAX_CACHE_SIZE = 10
    }
}
