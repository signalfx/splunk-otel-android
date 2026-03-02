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

package com.splunk.rum.common.otel.internal

import io.opentelemetry.api.common.AttributeKey

object GlobalRumConstants {

    /**
     * General
     */

    const val RUM_TRACER_NAME = "SplunkRum"
    const val EVENT_NAME = "event.name"
    const val DEFAULT_LOG_EVENT_NAME = "splunk.log"
    const val LOG_BODY_ATTRIBUTE = "body"
    const val SESSION_START_EVENT_NAME = "session.start"

    // Components
    const val COMPONENT_APP_LIFECYCLE = "app-lifecycle"
    const val COMPONENT_UI = "ui"
    const val COMPONENT_UI_LIFECYCLE = "ui"
    const val COMPONENT_SESSION_REPLAY = "session.replay"
    const val COMPONENT_HTTP = "http"
    const val COMPONENT_ERROR = "error"
    const val COMPONENT_CRASH = "crash"
    const val COMPONENT_CUSTOM_EVENT = "custom-event"
    const val COMPONENT_CUSTOM_WORKFLOW = "custom-workflow"

    // Attribute keys
    val LOG_EVENT_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey(EVENT_NAME)
    val COMPONENT_KEY: AttributeKey<String> = AttributeKey.stringKey("component")
    val SESSION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("session.id")
    val PREVIOUS_SESSION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("session.previous_id")
    val SESSION_RUM_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.rumSessionId")
    val USER_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("user.anonymous_id")


    /**
     * Navigation instrumentation
     */

    const val DEFAULT_SCREEN_NAME = "unknown"

    const val NAVIGATION_SPAN_NAME = "Created"
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
    val SCREEN_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("screen.name")
    val LAST_SCREEN_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("last.screen.name")

    /**
     * Application lifecycle instrumentation
     */

    const val APP_LIFECYCLE_LOG_NAME = "device.app.lifecycle"

    // Application lifecycle states
    const val APP_STATE_CREATED = "created"
    const val APP_STATE_FOREGROUND = "foreground"
    const val APP_STATE_BACKGROUND = "background"

    // Attribute key
    val APP_STATE_KEY: AttributeKey<String> = AttributeKey.stringKey("android.app.state")

    /**
     * UI lifecycle instrumentation
     */

    const val UI_LIFECYCLE_LOG_NAME = "device.app.ui.lifecycle"

    // UI lifecycle types
    const val UI_LIFECYCLE_ACTIVITY_TYPE = "Activity"
    const val UI_LIFECYCLE_FRAGMENT_TYPE = "Fragment"

    // Attribute keys
    val ELEMENT_TYPE_KEY: AttributeKey<String> = AttributeKey.stringKey("device.app.ui.element.type")
    val ELEMENT_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("device.app.ui.element.name")
    val ELEMENT_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("device.app.ui.element.id")
    val LIFECYCLE_ACTION_KEY: AttributeKey<String> = AttributeKey.stringKey("device.app.ui.element.lifecycle.action")

    /**
     * Interaction instrumentation
     */

    // Interaction types
    const val INTERACTIONS_EVENT_NAME = "action"
    const val INTERACTIONS_ACTION_FOCUS = "focus"
    const val INTERACTIONS_ACTION_SOFT_KEYBOARD = "soft_keyboard"
    const val INTERACTIONS_ACTION_PHONE_BUTTON = "phone_button"
    const val INTERACTIONS_ACTION_DOUBLE_TAP = "double_tap"
    const val INTERACTIONS_ACTION_LONG_PRESS = "long_press"
    const val INTERACTIONS_ACTION_PINCH = "pinch"
    const val INTERACTIONS_ACTION_RAGE_TAP = "rage_tap"
    const val INTERACTIONS_ACTION_ROTATION = "rotation"
    const val INTERACTIONS_ACTION_TAP = "tap"

    // Attribute keys
    val INTERACTIONS_ACTION_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("action.name")
    val INTERACTIONS_TARGET_TYPE_KEY: AttributeKey<String> = AttributeKey.stringKey("target.type")
    val INTERACTIONS_TARGET_XPATH_KEY: AttributeKey<String> = AttributeKey.stringKey("target_xpath")
    val INTERACTIONS_TARGET_ELEMENT_KEY: AttributeKey<String> = AttributeKey.stringKey("target_element")

    /**
     * Session Replay instrumentation
     */

    const val SCRIPT_INSTANCE_LENGTH = 16
    const val SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME = "SessionReplayDataScopeName"
    const val SESSION_REPLAY_DATA_EVENT_NAME = "session_replay_data"
    const val SESSION_REPLAY_IS_RECORDING_EVENT_NAME = "splunk.sessionReplay.isRecording"
    const val SESSION_REPLAY_PROVIDER = "splunk"

    // Attribute key
    val SCRIPT_INSTANCE_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.scriptInstance")
    val SESSION_REPLAY_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.sessionReplay")
    val SESSION_REPLAY_TOTAL_CHUNKS_KEY: AttributeKey<Double> = AttributeKey.doubleKey("rr-web.total-chunks")
    val SESSION_REPLAY_CHUNK_KEY: AttributeKey<Double> = AttributeKey.doubleKey("rr-web.chunk")
    val SESSION_REPLAY_EVENT_INDEX_KEY: AttributeKey<Long> = AttributeKey.longKey("rr-web.event")
    val SESSION_REPLAY_OFFSET_KEY: AttributeKey<Double> = AttributeKey.doubleKey("rr-web.offset")
    val SESSION_REPLAY_SEGMENT_METADATA_KEY: AttributeKey<String> = AttributeKey.stringKey("segmentMetadata")

    /**
     * Custom event and workflow instrumentation
     */

    // Attribute keys
    val WORKFLOW_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("workflow.name")

    /**
     * Error/crash instrumentation
     */

    const val CRASH_INSTRUMENTATION_SCOPE_NAME = "io.opentelemetry.crash"
    const val ERROR_TRUE_VALUE = "true"

    // Attribute keys
    val ERROR_KEY: AttributeKey<String> = AttributeKey.stringKey("error")
    val APPLICATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("service.application_id")
    val APP_VERSION_CODE_KEY: AttributeKey<String> = AttributeKey.stringKey("service.version_code")
    val SPLUNK_BUILD_ID: AttributeKey<String> = AttributeKey.stringKey("splunk.build_id")

    /**
     * Network instrumentation
     */

    const val SERVER_TIMING_HEADER = "server-timing"

    // Attribute keys
    val LINK_SPAN_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("link.spanId")
    val LINK_TRACE_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("link.traceId")
    val HTTP_REQUEST_BODY_SIZE: AttributeKey<Long> = AttributeKey.longKey("http.request.body.size")
    val HTTP_RESPONSE_BODY_SIZE: AttributeKey<Long> = AttributeKey.longKey("http.response.body.size")
}
