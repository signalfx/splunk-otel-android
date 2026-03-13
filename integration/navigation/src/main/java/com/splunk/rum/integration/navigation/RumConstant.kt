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

package com.splunk.rum.integration.navigation

import io.opentelemetry.api.common.AttributeKey

internal object RumConstant {

    const val COMPONENT_NAVIGATION = "ui"

    const val NAVIGATION_LOG_EVENT_NAME = "device.app.ui.navigation"

    const val NAVIGATION_RESTARTED_SPAN_NAME = "Restarted"
    const val NAVIGATION_RESTORED_SPAN_NAME = "Restored"
    const val NAVIGATION_RESUMED_SPAN_NAME = "Resumed"
    const val NAVIGATION_PAUSED_SPAN_NAME = "Paused"
    const val NAVIGATION_STOPPED_SPAN_NAME = "Stopped"
    const val NAVIGATION_VIEW_DESTROYED_SPAN_NAME = "ViewDestroyed"
    const val NAVIGATION_DESTROYED_SPAN_NAME = "Destroyed"
    const val NAVIGATION_DETACHED_SPAN_NAME = "Detached"

    const val NAVIGATION_ACTIVITY_PRE_CREATED_EVENT = "activityPreCreated"
    const val NAVIGATION_ACTIVITY_CREATED_EVENT = "activityCreated"
    const val NAVIGATION_ACTIVITY_POST_CREATED_EVENT = "activityPostCreated"
    const val NAVIGATION_ACTIVITY_PRE_STARTED_EVENT = "activityPreStarted"
    const val NAVIGATION_ACTIVITY_STARTED_EVENT = "activityStarted"
    const val NAVIGATION_ACTIVITY_POST_STARTED_EVENT = "activityPostStarted"
    const val NAVIGATION_ACTIVITY_PRE_RESUMED_EVENT = "activityPreResumed"
    const val NAVIGATION_ACTIVITY_RESUMED_EVENT = "onActivityResumed"
    const val NAVIGATION_ACTIVITY_POST_RESUMED_EVENT = "activityPostResumed"
    const val NAVIGATION_ACTIVITY_PRE_PAUSED_EVENT = "activityPrePaused"
    const val NAVIGATION_ACTIVITY_PAUSED_EVENT = "onActivityPaused"
    const val NAVIGATION_ACTIVITY_POST_PAUSED_EVENT = "activityPostPaused"
    const val NAVIGATION_ACTIVITY_PRE_STOPPED_EVENT = "activityPreStopped"
    const val NAVIGATION_ACTIVITY_STOPPED_EVENT = "activityStopped"
    const val NAVIGATION_ACTIVITY_POST_STOPPED_EVENT = "activityPostStopped"
    const val NAVIGATION_ACTIVITY_PRE_DESTROYED_EVENT = "activityPreDestroyed"
    const val NAVIGATION_ACTIVITY_DESTROYED_EVENT = "activityDestroyed"
    const val NAVIGATION_ACTIVITY_POST_DESTROYED_EVENT = "activityPostDestroyed"

    const val NAVIGATION_FRAGMENT_PRE_ATTACHED_EVENT = "fragmentPreAttached"
    const val NAVIGATION_FRAGMENT_ATTACHED_EVENT = "fragmentAttached"
    const val NAVIGATION_FRAGMENT_PRE_CREATED_EVENT = "fragmentPreCreated"
    const val NAVIGATION_FRAGMENT_CREATED_EVENT = "fragmentCreated"
    const val NAVIGATION_FRAGMENT_VIEW_CREATED_EVENT = "fragmentViewCreated"
    const val NAVIGATION_FRAGMENT_STARTED_EVENT = "fragmentStarted"
    const val NAVIGATION_FRAGMENT_RESUMED_EVENT = "onFragmentResumed"
    const val NAVIGATION_FRAGMENT_PAUSED_EVENT = "onFragmentPaused"
    const val NAVIGATION_FRAGMENT_STOPPED_EVENT = "fragmentStopped"
    const val NAVIGATION_FRAGMENT_VIEW_DESTROYED_EVENT = "fragmentViewDestroyed"
    const val NAVIGATION_FRAGMENT_DESTROYED_EVENT = "fragmentDestroyed"
    const val NAVIGATION_FRAGMENT_DETACHED_EVENT = "fragmentDetached"

    // Attribute keys
    val NAVIGATION_ACTIVITY_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("activity.name")
    val NAVIGATION_FRAGMENT_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("fragment.name")
}
