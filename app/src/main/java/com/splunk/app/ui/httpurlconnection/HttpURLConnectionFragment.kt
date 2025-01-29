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
import android.widget.Toast
import com.cisco.android.common.utils.runOnBackgroundThread
import com.cisco.android.common.utils.runOnUiThread
import com.splunk.app.R
import com.splunk.app.databinding.FragmentHttpUrlConnectionBinding
import com.splunk.app.ui.BaseFragment
import com.splunk.rum.library.httpurlconnection.HttpUrlInstrumentationConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HttpURLConnectionFragment : BaseFragment<FragmentHttpUrlConnectionBinding>() {

    override val titleRes: Int = R.string.httpurlconnection_title
    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentHttpUrlConnectionBinding
        get() = FragmentHttpUrlConnectionBinding::inflate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schedule thread that closes un-ended spans for edge cases
        val executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleWithFixedDelay(
            HttpUrlInstrumentationConfig.getReportIdleConnectionRunnable(),
            0,
            HttpUrlInstrumentationConfig.getReportIdleConnectionInterval(),
            TimeUnit.MILLISECONDS
        )
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
    }

    /**
     * Demonstrates a custom url HttpURLConnection GET request
     */
    fun customUrlGet() {
        executeGet(viewBinding.customUrl.text.toString())
        showDoneToast("customUrlGet")
    }

    /**
     * Demonstrates a successful HttpURLConnection GET request
     */
    fun successfulGet() {
        executeGet("http://httpbin.org/get")
        showDoneToast("successfulGet")
    }

    /**
     * Demonstrates a HttpURLConnection GET request with no call to getInputStream
     * and thereby requiring either the disconnect or the harvester thread (if disconnect is not called)
     * to end the span. This test covers OPTIONS and TRACE requests too.
     */
    fun getWithoutInputStream() {
        executeGet("http://httpbin.org/get", false)
        showDoneToast("getWithoutInputStream")
    }

    /**
     * Demonstrates four concurrent HttpURLConnection GET request (running on different threads in parallel)
     * Helps test proper synchronization is achieved in our callback APIs code.
     */
    fun fourConcurrentGetRequests() {
        executeGet("http://httpbin.org/get")
        executeGet("http://google.com")
        executeGet("http://android.com")
        executeGet("http://httpbin.org/headers")
        showDoneToast("fourConcurrentGetRequests")
    }

    /**
     * Demonstrates a HttpURLConnection GET request with no call to getInputStream and disconnect
     * and thereby requiring the harvester thread to end the span.
     */
    fun sustainedConnection() {
        executeGet("http://httpbin.org/get", false, false)
        showDoneToast("sustainedConnection")
    }

    /**
     * Demonstrates a HttpURLConnection GET request with 20s wait after initial read
     * and thereby requiring the harvester thread to end the span.
     */
    fun stalledRequest() {
        executeGet("http://httpbin.org/get", false, true, true)
        showDoneToast("stalledRequest")
    }

    private fun executeGet(inputUrl: String, getInputStream: Boolean = true, disconnect: Boolean = true, stallRequest: Boolean = false) {
        runOnBackgroundThread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(inputUrl)
                connection = url.openConnection() as HttpURLConnection

                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                val readInput = if (getInputStream) connection.inputStream.bufferedReader().use { it.readText() } else ""
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
            val connection = URL("http://httpbin.org/status/404").openConnection() as HttpURLConnection
            try {
                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                val errorStream = connection.errorStream
                val readError = errorStream.bufferedReader().use { it.readText() }
                Log.v(TAG, "response code: " + responseCode + " response message: " + responseMessage +
                    " ErrorStream: " + readError)
                showDoneToast("unSuccessfulGet")
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
            val connection = URL("http://httpbin.org/post").openConnection() as HttpURLConnection
            connection.doOutput = true
            connection.requestMethod = "POST"
            try {
                val outputStream = connection.outputStream
                outputStream.bufferedWriter().use { out -> out.write("Writing test content to output stream!") }

                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                val inputStream = connection.inputStream
                val readInput = inputStream.bufferedReader().use { it.readText() }
                Log.v(TAG, "response code: " + responseCode + " response message: " + responseMessage +
                    " InputStream: " + readInput)
                showDoneToast("post")
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun showDoneToast(methodName: String) {
        runOnUiThread {
            Toast.makeText(context, getString(R.string.http_toast, methodName), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "HttpURLConnection"
    }
}
