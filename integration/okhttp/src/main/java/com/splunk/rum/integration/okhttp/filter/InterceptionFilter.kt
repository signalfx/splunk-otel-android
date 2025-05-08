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

package com.splunk.rum.integration.okhttp.filter

internal object InterceptionFilter {

    private val allowedHeaders = setOf(
        "a-im",
        "accept",
        "accept-charset",
        "accept-datetime",
        "accept-encoding",
        "accept-language",
        "access-control-request-method",
        "access-control-request-headers",
        "cache-control",
        "connection",
        "content-encoding",
        "content-length",
        "content-md5",
        "content-type",
        "cookie",
        "date",
        "expect",
        "forwarded",
        "from",
        "host",
        "http2-settings",
        "if-match",
        "if-modified-since",
        "if-none-match",
        "if-range",
        "if-unmodified-since",
        "max-forwards",
        "origin",
        "pragma",
        "prefer",
        "range",
        "referer",
        "te",
        "trailer",
        "transfer-encoding",
        "user-agent",
        "upgrade",
        "via",
        "warning"
    )

    fun isHeaderAllowed(name: String): Boolean = name.lowercase() in allowedHeaders
}
