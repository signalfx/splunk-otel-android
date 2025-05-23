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

package com.splunk.rum.integration.okhttp.interceptor

import com.cisco.android.common.utils.extensions.anyFast
import com.splunk.rum.integration.agent.api.network.SplunkNetworkRequest
import com.splunk.rum.integration.okhttp.model.SplunkChain
import okhttp3.Headers

/**
 * Interceptor that filters headers by given regexes.
 *
 * @param allowedHeaders Set of regexes that are used to filter headers.
 */
class SplunkHeadersInterceptor(val allowedHeaders: List<Regex>) : SplunkOkHttpInterceptor {

    constructor(allowedHeaders: Set<String>) : this(allowedHeaders.map { it.toRegex() })

    override fun onIntercept(original: SplunkChain, intercepted: SplunkNetworkRequest): SplunkNetworkRequest =
        intercepted.apply {
            requestHeaders = original.request.headers.parseAndFilter()
            responseHeaders = original.response?.headers?.parseAndFilter()
        }

    private fun Headers.parseAndFilter(): MutableMap<String, MutableList<String>> {
        val parsedHeaders = mutableMapOf<String, MutableList<String>>()

        forEach { header ->
            if (allowedHeaders.anyFast { it.matches(header.first) }) {
                val name = header.first.lowercase()
                val value = header.second

                if (name in parsedHeaders) {
                    parsedHeaders[name]?.add(value)
                } else {
                    parsedHeaders[name] = mutableListOf(value)
                }
            }
        }

        return parsedHeaders
    }
}
