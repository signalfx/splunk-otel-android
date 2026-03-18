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

package com.splunk.rum.integration.startup

import io.opentelemetry.api.common.AttributeKey

internal object RumConstants {

    const val COMPONENT_APP_START = "appstart"
    const val APP_START_SPAN_NAME = "AppStart"
    const val APP_START_INITIALIZE_SPAN_NAME = "SplunkRum.initialize"

    // Application start type
    const val APP_START_TYPE_COLD = "cold"
    const val APP_START_TYPE_WARM = "warm"
    const val APP_START_TYPE_HOT = "hot"

    // Attribute keys
    val APP_START_TYPE_KEY: AttributeKey<String> = AttributeKey.stringKey("start.type")
    val APP_START_CONFIG_SETTINGS_KEY: AttributeKey<String> = AttributeKey.stringKey("config_settings")
}
