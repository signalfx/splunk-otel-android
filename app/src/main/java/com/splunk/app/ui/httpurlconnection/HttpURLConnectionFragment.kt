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
import com.cisco.android.common.utils.runOnBackgroundThread
import com.splunk.app.R
import com.splunk.app.databinding.FragmentHttpUrlConnectionBinding
import com.splunk.app.ui.BaseFragment
import com.splunk.app.util.CommonUtils
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.navigation.extension.navigation
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpURLConnectionFragment : BaseFragment<FragmentHttpUrlConnectionBinding>() {

    override val titleRes: Int = R.string.httpurlconnection_title
    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentHttpUrlConnectionBinding
        get() = FragmentHttpUrlConnectionBinding::inflate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.customUrlGet.setOnClickListener { customUrlGet() }
        viewBinding.successfulGet.setOnClickListener { successfulGet() }
        viewBinding.unSuccessfulGet.setOnClickListener { unSuccessfulGet() }
        viewBinding.getWithoutInputStream.setOnClickListener { getWithoutInputStream() }
        viewBinding.fourConcurrentGetRequests.setOnClickListener { fourConcurrentGetRequests() }
        viewBinding.sustainedConnection.setOnClickListener { sustainedConnection() }
        viewBinding.stalledRequest.setOnClickListener { stalledRequest() }
        viewBinding.post.setOnClickListener { post() }
        viewBinding.serverTimingHeaderInResponse.setOnClickListener { serverTimingHeaderInResponse() }

        SplunkRum.instance.navigation.track("HttpUrlConnection")
    }

    /**
     * Demonstrates a custom url HttpURLConnection GET request
     */
    fun customUrlGet() {
        executeGet(viewBinding.customUrl.text.toString())
        CommonUtils.showDoneToast(context, "Custom Url Get")
    }

    /**
     * Demonstrates a successful HttpURLConnection GET request
     */
    fun successfulGet() {
        executeGet("https://httpbin.org/get")
        CommonUtils.showDoneToast(context, "Successful Get")
    }

    /**
     * Demonstrates a HttpURLConnection GET request with no call to getInputStream
     * and thereby requiring either the disconnect or the harvester thread (if disconnect is not called)
     * to end the span. This test covers OPTIONS and TRACE requests too.
     */
    fun getWithoutInputStream() {
        executeGet("https://httpbin.org/get", false)
        CommonUtils.showDoneToast(context, "Get Without InputStream")
    }

    /**
     * Demonstrates four concurrent HttpURLConnection GET request (running on different threads in parallel)
     * Helps test proper synchronization is achieved in our callback APIs code.
     */
    fun fourConcurrentGetRequests() {
        executeGet("https://httpbin.org/get")
        executeGet("https://google.com")
        executeGet("https://android.com")
        executeGet("https://httpbin.org/headers")
        CommonUtils.showDoneToast(context, "Four Concurrent Get Requests")
    }

    /**
     * Demonstrates a HttpURLConnection GET request with no call to getInputStream and disconnect
     * and thereby requiring the harvester thread to end the span.
     */
    fun sustainedConnection() {
        executeGet("https://httpbin.org/get", false, false)
        CommonUtils.showDoneToast(context, "Sustained Connection")
    }

    /**
     * Demonstrates a HttpURLConnection GET request with 20s wait after initial read
     * and thereby requiring the harvester thread to end the span.
     */
    fun stalledRequest() {
        executeGet("https://httpbin.org/get", false, true, true)
        CommonUtils.showDoneToast(context, "Stalled Request")
    }

    /**
     * Demonstrates addition of link.traceId and link.spanId attributes in span when
     * server-timing header is present in the response.
     */
    fun serverTimingHeaderInResponse() {
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

        CommonUtils.showDoneToast(context, "Server-Timing Header In Response")
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
                val url = URL(inputUrl)
                connection = url.openConnection() as HttpURLConnection

                // Add request header
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                val readInput = if (getInputStream) {
                    connection.inputStream.bufferedReader().use {
                        it.readText()
                    }
                } else {
                    ""
                }
                val readInputString = if (getInputStream) ("input Stream: " + readInput) else ""

                stallRequest.takeIf { it }?.let { Thread.sleep(20000) }

                Log.v(TAG, "response code: " + responseCode + " response message: " + responseMessage + readInputString)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.takeIf { disconnect }?.disconnect()
            }
        }
    }

    /**
     * Demonstrates an unsuccessful HttpURLConnection GET request
     */
    fun unSuccessfulGet() {
        runOnBackgroundThread {
            val connection = URL("https://httpbin.org/status/404").openConnection() as HttpURLConnection
            try {
                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                val errorStream = connection.errorStream
                val readError = errorStream.bufferedReader().use { it.readText() }
                Log.v(
                    TAG,
                    "response code: " + responseCode + " response message: " + responseMessage +
                        " ErrorStream: " + readError
                )
                CommonUtils.showDoneToast(context, "UnSuccessful Get")
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Demonstrates a HttpURLConnection POST request
     */
    fun post() {
        runOnBackgroundThread {
            val connection = URL("https://httpbin.org/post").openConnection() as HttpURLConnection
            connection.doOutput = true
            connection.requestMethod = "POST"
            try {
                val outputStream = connection.outputStream
                outputStream.bufferedWriter().use { out -> out.write("Writing test content to output stream!") }

                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                val inputStream = connection.inputStream
                val readInput = inputStream.bufferedReader().use { it.readText() }
                Log.v(
                    TAG,
                    "response code: " + responseCode + " response message: " + responseMessage +
                        " InputStream: " + readInput
                )
                CommonUtils.showDoneToast(context, "Post")
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        private const val TAG = "HttpURLConnection"
    }
}
