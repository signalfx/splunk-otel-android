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

package com.splunk.rum.integration.agent.api.network

import java.net.URL

/**
 * Data class representing a network request.
 *
 * @param url The URL of the request.
 * @param method The HTTP method of the request.
 * @param statusCode The HTTP status code of the response.
 * @param requestHeaders The HTTP headers of the request.
 * @param responseHeaders The HTTP headers of the response.
 */
data class SplunkNetworkRequest(
    var url: URL,
    var method: String,
    var statusCode: Int,
    var requestHeaders: MutableMap<String, MutableList<String>>?,
    var responseHeaders: MutableMap<String, MutableList<String>>?
)
