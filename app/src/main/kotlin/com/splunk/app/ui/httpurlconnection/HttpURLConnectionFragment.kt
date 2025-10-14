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

package com.splunk.app.ui.httpurlconnection

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.splunk.android.common.utils.runOnBackgroundThread
import com.splunk.app.R
import com.splunk.app.databinding.FragmentHttpUrlConnectionBinding
import com.splunk.app.extension.showDoneToast
import com.splunk.app.ui.BaseFragment
import java.net.HttpURLConnection
import java.net.URL

/**
 * A fragment that demonstrates various types of [HttpURLConnection] requests,
 * such as GET, POST, concurrent requests, and responses with `Server-Timing` headers.
 */
class HttpURLConnectionFragment : BaseFragment<FragmentHttpUrlConnectionBinding>() {

    override val titleRes: Int = R.string.httpurlconnection_title

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentHttpUrlConnectionBinding
        get() = FragmentHttpUrlConnectionBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            customUrlGet.setOnClickListener { customUrlGet() }
            successfulGet.setOnClickListener { successfulGet() }
            unSuccessfulGet.setOnClickListener { unSuccessfulGet() }
            getWithoutInputStream.setOnClickListener { getWithoutInputStream() }
            fourConcurrentGetRequests.setOnClickListener { fourConcurrentGetRequests() }
            sustainedConnection.setOnClickListener { sustainedConnection() }
            stalledRequest.setOnClickListener { stalledRequest() }
            post.setOnClickListener { post() }
            serverTimingHeaderInResponse.setOnClickListener { serverTimingHeaderInResponse() }
        }
    }

    /**
     * Demonstrates a custom url HttpURLConnection GET request
     */
    private fun customUrlGet() {
        executeGet(viewBinding.customUrl.text.toString())
        context?.showDoneToast(R.string.custom_url_get)
    }

    /**
     * Demonstrates a successful HttpURLConnection GET request
     */
    private fun successfulGet() {
        executeGet("https://httpbin.org/get")
        context?.showDoneToast(R.string.successful_get)
    }

    /**
     * Demonstrates an unsuccessful HttpURLConnection GET request
     */
    private fun unSuccessfulGet() {
        executeGet("https://httpbin.org/status/404")
        context?.showDoneToast(R.string.unsuccessful_get)
    }

    /**
     * Demonstrates a HttpURLConnection GET request with no call to getInputStream
     * and thereby requiring either the disconnect or the harvester thread (if disconnect is not called)
     * to end the span. This test covers OPTIONS and TRACE requests too.
     */
    private fun getWithoutInputStream() {
        executeGet("https://httpbin.org/get", false)
        context?.showDoneToast(R.string.get_without_input_stream)
    }

    /**
     * Demonstrates four concurrent HttpURLConnection GET request (running on different threads in parallel)
     * Helps test proper synchronization is achieved in our callback APIs code.
     */
    private fun fourConcurrentGetRequests() {
        executeGet("https://httpbin.org/get")
        executeGet("https://google.com")
        executeGet("https://android.com")
        executeGet("https://httpbin.org/headers")
        context?.showDoneToast(R.string.four_concurrent_get_requests)
    }

    /**
     * Demonstrates a HttpURLConnection GET request with no call to getInputStream and disconnect
     * and thereby requiring the harvester thread to end the span.
     */
    private fun sustainedConnection() {
        executeGet("https://httpbin.org/get", getInputStream = false, disconnect = false)
        context?.showDoneToast(R.string.sustained_connection)
    }

    /**
     * Demonstrates a HttpURLConnection GET request with 20s wait after initial read
     * and thereby requiring the harvester thread to end the span.
     */
    private fun stalledRequest() {
        executeGet("https://httpbin.org/get", getInputStream = false, disconnect = true, stallRequest = true)
        context?.showDoneToast(R.string.stalled_request)
    }

    /**
     * Demonstrates addition of link.traceId and link.spanId attributes in span when
     * server-timing header is present in the response.
     */
    private fun serverTimingHeaderInResponse() {
        // one valid Server-Timing header, link.traceId and link.spanId attributes will be populated correctly
        executeGet(
            "https://httpbin.org/response-headers?Server-Timing=traceparent;desc='00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01'"
        )

        // invalid Server-Timing header, link.traceId and link.spanId attributes will not be set
        executeGet("https://httpbin.org/response-headers?Server-Timing=incorrectSyntax")

        // two valid Server-Timing headers, last one wins - link.traceId and link.spanId attributes will be populated
        // with the values from last valid header found
        executeGet(
            "https://httpbin.org/response-headers" +
                "?Server-Timing=traceparent;desc=\"00-00000000000000000000000000000001-0000000000000001-01\"" +
                "&Server-Timing=traceparent;desc=\"00-00000000000000000000000000000002-0000000000000002-01\""
        )

        context?.showDoneToast(R.string.server_timing_header_in_response)
    }

    /**
     * Demonstrates a HttpURLConnection POST request
     */
    private fun post() {
        executePost(
            inputUrl = "https://httpbin.org/post",
            requestBody = "Writing test content to output stream!"
        )

        context?.showDoneToast(R.string.post)
    }

    private fun executeGet(
        inputUrl: String,
        getInputStream: Boolean = true,
        disconnect: Boolean = true,
        stallRequest: Boolean = false
    ) {
        runOnBackgroundThread {
            var connection: HttpURLConnection? = null
            try {
                connection = inputUrl.openHttpConnection()
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage

                val responseBody = if (responseCode.isSuccessful && getInputStream) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                if (stallRequest) {
                    Thread.sleep(STALL_DURATION_MS)
                }

                Log.v(
                    TAG,
                    "response code: $responseCode response message: $responseMessage response body: $responseBody"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (disconnect) {
                    connection?.disconnect()
                }
            }
        }
    }

    private fun executePost(inputUrl: String, requestBody: String) {
        runOnBackgroundThread {
            var connection: HttpURLConnection? = null
            try {
                connection = inputUrl.openHttpConnection()
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "text/plain")
                connection.setRequestProperty("Accept", "application/json")

                connection.outputStream.bufferedWriter().use { it.write(requestBody) }

                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }

                Log.v(
                    TAG,
                    "response code: $responseCode response message: $responseMessage response body: $responseBody"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun String.openHttpConnection(): HttpURLConnection = URL(this).openConnection() as HttpURLConnection

    private val Int.isSuccessful: Boolean
        get() = this in 200..299

    companion object {
        private const val TAG = "HttpURLConnection"
        private const val STALL_DURATION_MS = 20_000L
    }
}
