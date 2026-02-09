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
 * Represents the lifecycle actions for Activities and Fragments.
 * Each action corresponds to a lifecycle callback.
 */
internal enum class LifecycleAction(val attributeValue: String) {
    // Activity and Fragment common actions
    CREATED("created"),
    STARTED("started"),
    RESUMED("resumed"),
    PAUSED("paused"),
    STOPPED("stopped"),
    DESTROYED("destroyed"),

    // Activity Pre/Post actions (API 29+)
    PRE_CREATED("pre_created"),
    POST_CREATED("post_created"),
    PRE_STARTED("pre_started"),
    POST_STARTED("post_started"),
    PRE_RESUMED("pre_resumed"),
    POST_RESUMED("post_resumed"),
    PRE_PAUSED("pre_paused"),
    POST_PAUSED("post_paused"),
    PRE_STOPPED("pre_stopped"),
    POST_STOPPED("post_stopped"),
    PRE_DESTROYED("pre_destroyed"),
    POST_DESTROYED("post_destroyed"),

    // Fragment-specific actions
    PRE_ATTACHED("pre_attached"),
    ATTACHED("attached"),
    DETACHED("detached"),
    VIEW_CREATED("view_created"),
    VIEW_DESTROYED("view_destroyed")
}
