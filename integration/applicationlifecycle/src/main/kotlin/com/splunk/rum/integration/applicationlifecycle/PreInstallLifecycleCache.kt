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

/** Caches lifecycle events until installation and marks where the last Activity closed. */
internal class PreInstallLifecycleCache {

    private val events = mutableListOf<ApplicationLifecycleData>()
    private var firstEventIndexAfterLastClose = 0

    fun add(event: ApplicationLifecycleData) {
        if (events.size < MAX_CACHE_SIZE) events += event
    }

    fun markAppClosed() {
        firstEventIndexAfterLastClose = events.size
    }

    /** Without background sessions, report only events recorded after the last Activity closed. */
    fun eventsToReport(allowBackgroundSessionCreation: Boolean): List<ApplicationLifecycleData> =
        events.drop(if (allowBackgroundSessionCreation) 0 else firstEventIndexAfterLastClose)

    fun clear() {
        events.clear()
        firstEventIndexAfterLastClose = 0
    }

    private companion object {
        const val MAX_CACHE_SIZE = 10
    }
}
