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

package com.splunk.rum.integration.lifecycle

import io.opentelemetry.api.common.AttributeKey

internal object RumConstants {

    const val COMPONENT_UI_LIFECYCLE = "ui"
    const val UI_LIFECYCLE_LOG_NAME = "app.ui.lifecycle"

    // UI lifecycle types
    const val UI_LIFECYCLE_ACTIVITY_TYPE = "Activity"
    const val UI_LIFECYCLE_FRAGMENT_TYPE = "Fragment"

    // Attribute keys
    val ELEMENT_TYPE_KEY: AttributeKey<String> = AttributeKey.stringKey("element.type")
    val ELEMENT_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("element.name")
    val ELEMENT_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("element.id")
    val LIFECYCLE_ACTION_KEY: AttributeKey<String> = AttributeKey.stringKey("lifecycle.action")
}
