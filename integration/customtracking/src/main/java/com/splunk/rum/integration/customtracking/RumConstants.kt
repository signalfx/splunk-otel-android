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

package com.splunk.rum.integration.customtracking

import io.opentelemetry.api.common.AttributeKey

internal object RumConstants {

    const val ERROR_TRUE_VALUE = "true"
    const val COMPONENT_CUSTOM_EVENT = "custom-event"
    const val COMPONENT_CUSTOM_WORKFLOW = "custom-workflow"

    // Attribute keys
    val WORKFLOW_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("workflow.name")
}
