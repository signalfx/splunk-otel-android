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

package com.splunk.rum.integration.okhttp

import com.cisco.android.common.utils.MutableListObserver
import com.splunk.rum.integration.agent.api.network.SplunkNetworkRequest
import com.splunk.rum.integration.okhttp.filter.InterceptionFilter
import com.splunk.rum.integration.okhttp.interceptor.SplunkOkHttpInterceptor
import com.splunk.rum.integration.okhttp.model.SplunkChain
import java.io.IOException
import java.net.URL
import okhttp3.Connection
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class OkHttpConnector(
    private val client: OkHttpClient.Builder,
    private val listener: Listener,
    interceptorsObserver: MutableListObserver.Observer<SplunkOkHttpInterceptor>
) {

    val interceptors: MutableList<SplunkOkHttpInterceptor> = MutableListObserver(
        list = mutableListOf(),
        observer = interceptorsObserver
    )

    private var networkInterceptor: Interceptor? = null

    fun register() {
        if (networkInterceptor != null) {
            return
        }

        val newNetworkInterceptor = Interceptor { chain -> processChain(chain) }

        networkInterceptor = newNetworkInterceptor
        client.addNetworkInterceptor(newNetworkInterceptor)
    }

    fun unregister() {
        if (networkInterceptor != null) {
            client.networkInterceptors().remove(networkInterceptor)
        }
    }

    private fun onIntercept(original: SplunkChain, prefilteredInterceptedRequest: SplunkNetworkRequest) {
        val processedRequest = interceptors.fold(prefilteredInterceptedRequest) { intercepted, interceptor ->
            interceptor.onIntercept(original, intercepted) ?: return
        }

        listener.onRequest(processedRequest)
    }

    private fun processChain(chain: Interceptor.Chain): Response {
        val start = System.currentTimeMillis()
        val request = chain.request()
        val connection = chain.connection()

        val response = try {
            chain.proceed(request)
        } catch (exception: IOException) {
            onIntercept(
                original = SplunkChain(chain, null),
                prefilteredInterceptedRequest = parseRequestAndResponse(
                    start = start,
                    duration = System.currentTimeMillis() - start,
                    request = request,
                    response = null,
                    connection = connection
                )
            )

            throw exception
        }

        onIntercept(
            original = SplunkChain(chain, response),
            prefilteredInterceptedRequest = parseRequestAndResponse(
                start = start,
                duration = System.currentTimeMillis() - start,
                request = request,
                response = response,
                connection = connection
            )
        )

        return response
    }

    private fun parseRequestAndResponse(
        start: Long,
        duration: Long,
        request: Request,
        response: Response?,
        connection: Connection?
    ): SplunkNetworkRequest = SplunkNetworkRequest(
        url = request.parseURL(),
        method = request.parseMethod(),
        statusCode = response?.parseStatusCode() ?: UNKNOWN_STATUS_CODE,
        requestHeaders = request.parseHeaders(),
        responseHeaders = response?.parseHeaders() ?: mutableMapOf()
    )

    private fun Request.parseURL(): URL = url.toUrl()

    private fun Request.parseMethod(): String = method.uppercase()

    private fun Connection.parseProtocol(): String = protocol().toString()

    private fun Response.parseStatusCode(): Int = code

    private fun Response.isCached(): Boolean = cacheResponse != null

    private fun Request.parseHeaders(): MutableMap<String, MutableList<String>> = headers.parse()

    private fun Response.parseHeaders(): MutableMap<String, MutableList<String>> = headers.parse()

    private fun Headers.parse(): MutableMap<String, MutableList<String>> {
        val parsedHeaders = mutableMapOf<String, MutableList<String>>()

        forEach { allowedHeader ->
            val name = allowedHeader.first.lowercase()
            if (InterceptionFilter.isHeaderAllowed(name)) {
                val value = allowedHeader.second

                if (name in parsedHeaders) {
                    parsedHeaders[name]?.add(value)
                } else {
                    parsedHeaders[name] = mutableListOf(value)
                }
            }
        }

        return parsedHeaders
    }

    internal interface Listener {
        fun onRequest(request: SplunkNetworkRequest)
    }

    private companion object {
        const val UNKNOWN_STATUS_CODE = -1
    }
}
