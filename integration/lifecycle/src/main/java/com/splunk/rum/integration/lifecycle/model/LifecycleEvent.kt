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

package com.splunk.rum.integration.lifecycle.model

/**
 * Represents a lifecycle event for Activities or Fragments.
 * Contains all the data needed to emit an OTel lifecycle event.
 */
internal sealed class LifecycleEvent {
    abstract val elementName: String
    abstract val elementType: String
    abstract val action: LifecycleAction
    abstract val timestamp: Long
    abstract val elementId: String

    /**
     * Lifecycle event for an Activity.
     *
     * @property elementName The simple class name of the Activity (e.g., "MainActivity")
     * @property action The lifecycle action that occurred
     * @property timestamp The timestamp when the event occurred (milliseconds since epoch)
     * @property elementId A unique identifier for this Activity instance
     */
    data class Activity(
        override val elementName: String,
        override val action: LifecycleAction,
        override val timestamp: Long,
        override val elementId: String
    ) : LifecycleEvent() {
        override val elementType = "Activity"
    }

    /**
     * Lifecycle event for a Fragment.
     *
     * @property elementName The simple class name of the Fragment (e.g., "HomeFragment")
     * @property action The lifecycle action that occurred
     * @property timestamp The timestamp when the event occurred (milliseconds since epoch)
     * @property elementId A unique identifier for this Fragment instance
     * @property parentActivity The simple class name of the parent Activity, if available
     */
    data class Fragment(
        override val elementName: String,
        override val action: LifecycleAction,
        override val timestamp: Long,
        override val elementId: String,
        val parentActivity: String? = null
    ) : LifecycleEvent() {
        override val elementType = "Fragment"
    }
}
