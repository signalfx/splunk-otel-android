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

package com.splunk.rum.integration.okhttp.model

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Wrapper for [Interceptor.Chain] that can be used to read intercepted request and response.
 */
open class SplunkChain(private val chain: Interceptor.Chain, private val processedResponse: Response?) {

    val call: Call
        get() = chain.call()

    val connectTimeoutMillis: Int
        get() = chain.connectTimeoutMillis()

    val connection: Connection?
        get() = chain.connection()

    val readTimeoutMillis: Int
        get() = chain.readTimeoutMillis()

    val request: Request
        get() = chain.request()

    val response: Response?
        get() = processedResponse

    val writeTimeoutMillis: Int
        get() = chain.writeTimeoutMillis()
}
