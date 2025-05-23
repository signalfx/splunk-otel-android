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

package com.splunk.rum.integration.agent.api.attributes

import io.opentelemetry.android.export.SpanDataModifier
import io.opentelemetry.api.common.AttributeKey

/**
 * This class hold [AttributeKey]s for standard RUM-related attributes that are not in the
 * OpenTelemetry [io.opentelemetry.semconv.SemanticAttributes] definitions.
 */
@Deprecated("This class will be removed in a future release")
object StandardAttributes {

    /**
     * The version of your app. Useful for adding to global attributes.
     */
    @JvmStatic
    val APP_VERSION: AttributeKey<String> = AttributeKey.stringKey("app.version")

    /**
     * The build type of your app (typically one of debug or release). Useful for adding to global attributes.
     */
    @JvmStatic
    val APP_BUILD_TYPE: AttributeKey<String> = AttributeKey.stringKey("app.build.type")

    /**
     * Full HTTP client request URL in the form `scheme://host[:port]/path?query[#fragment]`.
     * Useful for span data filtering with the [SpanDataModifier].
     */
    @JvmStatic
    val HTTP_URL: AttributeKey<String> = AttributeKey.stringKey("http.url")

    @JvmStatic
    val PREVIOUS_SESSION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.rum.previous_session_id")

    @JvmStatic
    val SESSION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.rumSessionId")
}
