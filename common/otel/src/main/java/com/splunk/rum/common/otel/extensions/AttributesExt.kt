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

/**
 * Appends attributes directly to a StringBuilder without creating intermediate String objects.
 *
 * @param builder The StringBuilder to append to.
 * @param separator The separator between key-value pairs. Default is ", ".
 * @param prefix The prefix before the first attribute. Default is "[".
 * @param postfix The postfix after the last attribute. Default is "]".
 */
fun Attributes.appendTo(builder: StringBuilder, separator: String = ", ", prefix: String = "[", postfix: String = "]") {
    builder.append(prefix)
    var first = true
    forEach { key, value ->
        if (!first) builder.append(separator)
        builder.append(key.key).append("=").append(value)
        first = false
    }
    builder.append(postfix)
}
