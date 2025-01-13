/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.app.lib

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.net.URL

object ZipkinCommunicator {
    private val interceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
    private val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()

    private const val ZIPKIN_URL = "http://${TestConstants.ZIPKIN_URL}:9411/api/v2/traces"
    private val request = Request.Builder()
        .url(ZIPKIN_URL)
        .build()

    fun getAllTraces(): String {
        var responseString: String = ""

        runCatching {
            client.newCall(request).execute()
        }.onFailure { error ->
            throw Exception("Unable to communicate with Zipkin. Error: $error")
        }.onSuccess { response ->
            responseString = response.body?.string() ?: ""
        }

        return responseString
    }

    fun getAllTracesBySpanName(spanName: String, duration: Long): String {
        var responseString: String = ""
        // TODO - Add lookback into the request builder once spans are working again.
        runCatching {
            request.newBuilder().url(URL("${TestConstants.ZIPKIN_URL}/?spanName=$spanName"))
            client.newCall(request).execute()
        }.onFailure { error ->
            throw Exception("Unable to communicate with Zipkin. Error: $error")
        }.onSuccess { response ->
            responseString = response.body?.string() ?: ""
        }

        return responseString
    }

    /**
     * There isn't a great way to clear the Zipkin database, so we can limit our results using
     * Zipkin's Look-back option. Here we use a test start time and calculate an end time and look
     * back for that duration only.
     */
    fun getTestDuration(startTime: Long): Long = System.currentTimeMillis() - startTime + 1000
}
