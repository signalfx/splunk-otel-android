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
    const val DEFAULT_SCREEN_NAME = "unknown"
    const val SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME = "SessionReplayDataScopeName"

    // Navigation
    const val NAVIGATION_SPAN_NAME = "Created"
    const val APP_START_SPAN_NAME = "AppStart"

    // Components
    const val COMPONENT_HTTP = "http"
    const val COMPONENT_ERROR = "error"
    const val COMPONENT_CRASH = "crash"
    const val COMPONENT_CUSTOM_WORKFLOW = "custom-workflow"

    // Application lifecycle states
    const val APP_STATE_CREATED = "created"
    const val APP_STATE_FOREGROUND = "foreground"
    const val APP_STATE_BACKGROUND = "background"

    // Attribute keys
    val LOG_EVENT_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey(EVENT_NAME)
    val COMPONENT_KEY: AttributeKey<String> = AttributeKey.stringKey("component")
    val SESSION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("session.id")
    val PREVIOUS_SESSION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("session.previous_id")
    val SESSION_RUM_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.rumSessionId")
    val USER_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("user.anonymous_id")
    val SCREEN_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("screen.name")
    val LAST_SCREEN_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("last.screen.name")
    val SCRIPT_INSTANCE_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.scriptInstance")
    val APP_STATE_KEY: AttributeKey<String> = AttributeKey.stringKey("android.app.state")

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
