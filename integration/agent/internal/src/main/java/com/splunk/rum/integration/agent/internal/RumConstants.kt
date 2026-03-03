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

package com.splunk.rum.integration.agent.internal

import io.opentelemetry.api.common.AttributeKey

internal object RumConstants {
    const val SESSION_START_EVENT_NAME = "session.start"

    // Attribute key
    val SESSION_RUM_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.rumSessionId")
    val USER_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("user.anonymous_id")
    val SCRIPT_INSTANCE_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.scriptInstance")
    val APPLICATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("service.application_id")
    val APP_VERSION_CODE_KEY: AttributeKey<String> = AttributeKey.stringKey("service.version_code")
    val SPLUNK_BUILD_ID: AttributeKey<String> = AttributeKey.stringKey("splunk.build_id")
}
