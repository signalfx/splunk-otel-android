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

package com.splunk.rum.integration.sessionreplay.index

import java.time.Instant
import java.util.TreeMap

internal class TimeIndex<T> {
    private val timeMap = TreeMap<Instant, T>()

    fun put(value: T) {
        @Suppress("NewApi") //Requires API 26 or core library desugaring
        timeMap[Instant.now()] = value
    }

    fun putAt(time: Instant, value: T) {
        @Suppress("NewApi") //Requires API 26 or core library desugaring
        timeMap[time] = value
    }

    fun getAt(time: Instant): T? = timeMap.floorEntry(time)?.value
}
