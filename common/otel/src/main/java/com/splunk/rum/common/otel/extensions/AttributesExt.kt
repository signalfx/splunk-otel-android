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

package com.splunk.rum.common.otel.extensions

import io.opentelemetry.api.common.Attributes

fun Attributes.joinToString(
    separator: String = ",",
    prefix: String = "",
    postfix: String = "",
    transform: (String, Any) -> CharSequence = { key, value -> "$key=$value" }
): String {
    val attributes = this
    val attributesSize = size()

    return buildString {
        append(prefix)

        var i = 0

        attributes.forEach { key, value ->
            append(transform(key.key, value))

            if (++i < attributesSize) {
                append(separator)
            }
        }

        append(postfix)
    }
}
