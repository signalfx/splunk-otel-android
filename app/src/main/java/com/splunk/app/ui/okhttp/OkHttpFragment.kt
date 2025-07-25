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

package com.splunk.app.ui.okhttp

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.createBitmap
import com.cisco.android.common.utils.extensions.safeSchedule
import com.cisco.android.common.utils.runOnBackgroundThread
import com.splunk.app.R
import com.splunk.app.databinding.FragmentOkhttpBinding
import com.splunk.app.extension.showDoneToast
import com.splunk.app.ui.BaseFragment
import com.splunk.app.util.ApiVariant
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.navigation.extension.navigation
import com.splunk.rum.integration.okhttp3.manual.extension.createRumOkHttpCallFactory
import com.splunk.rum.integration.okhttp3.manual.extension.okHttpManualInstrumentation
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink

/**
 * A fragment demonstrating various OkHttp3 use cases and integrations with Splunk RUM instrumentation.
 *
 * This fragment provides buttons in the UI that allow users to trigger different types of HTTP requests
 * using OkHttp. It showcases a wide range of real-world scenarios.
 *
 */
class OkHttpFragment : BaseFragment<FragmentOkhttpBinding>() {

    override val titleRes: Int = R.string.okhttp_title

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentOkhttpBinding
        get() = FragmentOkhttpBinding::inflate

    private val retryInterceptor = Interceptor { chain: Interceptor.Chain ->
        val request = chain.request()
        var response = chain.proceed(request)

        var tryCount = 0
        while (response.code == 503 && tryCount < 2) {
            tryCount++

            // retry the request
            response.close()
            response = chain.proceed(request)
        }
        return@Interceptor response
    }

    private var client: Call.Factory = OkHttpClient()
    private var retryClient: Call.Factory = retryClient()
    private var cachedClient: Call.Factory = cachedClient()
    private var manualInstrumentationApiType: ApiVariant? = null

    private fun retryClient(): OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .addInterceptor(
            retryInterceptor
        )
        .build()

    private fun cachedClient(): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(activity?.cacheDir, DISK_CACHE_FOLDER), DISK_CACHE_SIZE))
        .build()

    /**
     * Returns an instrumented [Call.Factory] for manual OkHttp instrumentation using the Latest API.
     */
    private fun instrumentedCallFactoryLatestAPI(client: OkHttpClient): Call.Factory =
        SplunkRum.instance.okHttpManualInstrumentation.buildOkHttpCallFactory(client)

    /**
     * Returns an instrumented [Call.Factory] for manual OkHttp instrumentation using the Legacy API.
     */
    private fun instrumentedCallFactoryLegacyAPI(client: OkHttpClient): Call.Factory =
        SplunkRum.instance.createRumOkHttpCallFactory(client)

    private val executor = Executors.newScheduledThreadPool(1)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val manualInstrumentationOptionsVisibility =
            if ((client as? OkHttpClient)?.networkInterceptors?.isEmpty() == true) {
                View.VISIBLE
            } else {
                View.GONE
            }

        with(viewBinding) {
            manualInstrumentationApiSelection.visibility = manualInstrumentationOptionsVisibility
            apiVariantRadioGroup.visibility = manualInstrumentationOptionsVisibility

            apiVariantRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.latestApi -> {
                        manualInstrumentationApiType = ApiVariant.LATEST
                        client = instrumentedCallFactoryLatestAPI(OkHttpClient())
                        retryClient = instrumentedCallFactoryLatestAPI(retryClient())
                        cachedClient = instrumentedCallFactoryLatestAPI(cachedClient())
                    }
                    R.id.legacyApi -> {
                        manualInstrumentationApiType = ApiVariant.LEGACY
                        client = instrumentedCallFactoryLegacyAPI(OkHttpClient())
                        retryClient = instrumentedCallFactoryLegacyAPI(retryClient())
                        cachedClient = instrumentedCallFactoryLegacyAPI(cachedClient())
                    }
                }
            }
        }

        viewBinding.synchronousGet.setOnClickListener { synchronousGet() }
        viewBinding.asynchronousGet.setOnClickListener { asynchronousGet() }
        viewBinding.concurrentAsynchronousGet.setOnClickListener { concurrentAsynchronousGet() }
        viewBinding.parentContextPropagationInAsyncGet.setOnClickListener { parentContextPropagationInAsyncGet() }
        viewBinding.unSuccessfulGet.setOnClickListener { unsuccessfulGet() }
        viewBinding.retryRequest.setOnClickListener { retryRequest() }
        viewBinding.multipleHeaders.setOnClickListener { multipleHeaders() }
        viewBinding.serverTimingHeaderInResponse.setOnClickListener { serverTimingHeaderInResponse() }
        viewBinding.postMarkdown.setOnClickListener { postMarkdown() }
        viewBinding.postStreaming.setOnClickListener { postStreaming() }
        viewBinding.postFile.setOnClickListener { postFile() }
        viewBinding.postFormParameters.setOnClickListener { postFormParameters() }
        viewBinding.postMutlipartRequest.setOnClickListener { postMutlipartRequest() }
        viewBinding.networkError.setOnClickListener { networkError() }
        viewBinding.responseCaching.setOnClickListener { responseCaching() }
        viewBinding.canceledCall.setOnClickListener { canceledCall() }

        SplunkRum.instance.navigation.track("OkHttp")
    }

    /**
     * Demonstrates a synchronous successful okhttp GET - Download a file, print its headers, and print its response body as a string.
     */
    private fun synchronousGet() {
        executeGetRequest("https://publicobject.com/helloworld.txt")
        context?.showDoneToast(R.string.synchronous_get)
    }

    /**
     * Demonstrates an asynchronous successful okhttp GET.
     * Download a file on a worker thread, and get called back when the response is readable.
     * The callback is made after the response headers are ready.
     */
    private fun asynchronousGet() {
        executeAsynchronousGet("https://httpbin.org/robots.txt")
        context?.showDoneToast(R.string.asynchronous_get)
    }

    /**
     * Demonstrates multiple successful asynchronous okhttp GET operations to the same url in parallel.
     */
    private fun concurrentAsynchronousGet() {
        val url = "https://httpbin.org/headers"
        executeAsynchronousGet(url)
        executeAsynchronousGet(url)
        context?.showDoneToast(R.string.concurrent_asynchronous_get)
    }

    /**
     * Demonstrates correct context propagation for asynchronous requests when parent context exists.
     */

    private fun parentContextPropagationInAsyncGet() {
        val lock = CountDownLatch(1)

        val openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder().build())
            .build()

        val span = openTelemetry.getTracer("Test Tracer").spanBuilder("A Span").startSpan()

        span!!.makeCurrent().use { _ ->

            val client: Call.Factory = when (manualInstrumentationApiType) {
                ApiVariant.LATEST -> instrumentedCallFactoryLatestAPI(contextPropagationClient(span))
                ApiVariant.LEGACY -> instrumentedCallFactoryLegacyAPI(contextPropagationClient(span))
                null -> contextPropagationClient(span)
            }

            val request = Request.Builder()
                .url("https://publicobject.com/helloworld.txt")
                .build()

            client.newCall(request).enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        // Verify that the original caller's context is the current one here.
                        if (span == Span.current()) {
                            Log.d(
                                TAG,
                                "Testing parent context propagation in async get - contexts are same as expected."
                            )
                        } else {
                            Log.e(
                                TAG,
                                "Testing parent context propagation in async get - Contexts are unexpectedly different."
                            )
                        }
                        lock.countDown()
                    }
                }
            )
        }
        lock.await()
        span.end()

        context?.showDoneToast(R.string.parent_context_propagation_async_get)
    }

    private fun contextPropagationClient(span: Span): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            Interceptor { chain: Interceptor.Chain ->
                val currentSpan = Span.current().spanContext
                // Verify context propagation.
                if (span.spanContext.traceId == currentSpan.traceId) {
                    Log.d(TAG, "Testing parent context propagation in async get - trace id's are same as expected.")
                } else {
                    Log.e(
                        TAG,
                        "Testing parent context propagation in async get - trace id's are unexpectedly not same."
                    )
                }
                chain.proceed(chain.request())
            }
        )
        .build()

    /**
     * Demonstrates an unsuccessful okhttp GET.
     */
    private fun unsuccessfulGet() {
        executeGetRequest("https://httpbin.org/status/404")
        context?.showDoneToast(R.string.unsuccessful_get)
    }

    /**
     * Demonstrates a request i.e being retried two additional times.
     * http.request.resend_count is being set on each retried request.
     */
    private fun retryRequest() {
        executeGetRequest("https://httpbin.org/status/503", true)
        context?.showDoneToast(R.string.retry_request)
    }

    /**
     * Typically HTTP headers work like a Map<String, String>: each field has one value or none.
     * But some headers permit multiple values, like Guava’s Multimap.
     * For example, it’s legal and common for an HTTP response to supply multiple Vary headers.
     */
    private fun multipleHeaders() {
        val request = Request.Builder()
            .url("https://api.github.com/repos/square/okhttp/issues")
            .header("User-Agent", "OkHttp Headers.java")
            .addHeader("Accept", "application/json; q=0.5")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .build()

        runOnBackgroundThread {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                Log.v(TAG, "Server: ${response.header("Server")}")
                Log.v(TAG, "Date: ${response.header("Date")}")
                Log.v(TAG, "Vary: ${response.headers("Vary")}")
            }
        }

        context?.showDoneToast(R.string.multiple_headers)
    }

    /**
     * Demonstrates addition of link.traceId and link.spanId attributes in span when
     * server-timing header is present in the response.
     */
    private fun serverTimingHeaderInResponse() {
        // one valid Server-Timing header, link.traceId and link.spanId attributes will be populated correctly
        executeGetRequest(
            "https://httpbin.org/response-headers?Server-Timing=traceparent;desc='00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01'"
        )

        // invalid Server-Timing header, link.traceId and link.spanId attributes will not be set
        executeGetRequest(
            "https://httpbin.org/response-headers?Server-Timing=incorrectSyntax"
        )

        // two valid Server-Timing headers, last one wins - link.traceId and link.spanId attributes will be populated
        // with the values from last valid header found
        executeGetRequest(
            "https://httpbin.org/response-headers" +
                "?Server-Timing=traceparent;desc=\"00-00000000000000000000000000000001-0000000000000001-01\"" +
                "&Server-Timing=traceparent;desc=\"00-00000000000000000000000000000002-0000000000000002-01\""
        )

        context?.showDoneToast(R.string.server_timing_header_in_response)
    }

    /**
     * Use an HTTP POST to send a request body to a service.
     * This example posts a markdown document to a web service that renders markdown as HTML.
     */
    private fun postMarkdown() {
        val postBody = """
        |Releases
        |--------
        |
        | * _1.0_ May 6, 2013
        | * _1.1_ June 15, 2013
        | * _1.2_ August 11, 2013
        |
        """.trimMargin()

        executePostRequest("https://api.github.com/markdown/raw", postBody.toRequestBody(MEDIA_TYPE_MARKDOWN))
        context?.showDoneToast(R.string.post_markdown)
    }

    /**
     * Here we POST a request body as a stream. The content of this request body is being generated
     * as it’s being written. This example streams directly into the Okio buffered sink.
     */
    private fun postStreaming() {
        val requestBody = object : RequestBody() {
            override fun contentType() = MEDIA_TYPE_MARKDOWN

            override fun writeTo(sink: BufferedSink) {
                sink.writeUtf8("Numbers\n")
                sink.writeUtf8("-------\n")
                for (i in 2..997) {
                    sink.writeUtf8(String.format(" * $i = ${factor(i)}\n"))
                }
            }

            private fun factor(n: Int): String {
                for (i in 2 until n) {
                    val x = n / i
                    if (x * i == n) return "${factor(x)} × $i"
                }
                return n.toString()
            }
        }

        executePostRequest("https://api.github.com/markdown/raw", requestBody)
        context?.showDoneToast(R.string.post_streaming)
    }

    /**
     * It’s easy to use a file as a request body.
     */
    private fun postFile() {
        val file = File(activity?.filesDir, "README.md")

        file.writeText(
            """
            |Releases
            |--------
            |
            | * _1.0_ May 6, 2013
            | * _1.1_ June 15, 2013
            | * _1.2_ August 11, 2013
            |
            """.trimMargin()
        )

        executePostRequest("https://api.github.com/markdown/raw", file.asRequestBody(MEDIA_TYPE_MARKDOWN))
        context?.showDoneToast(R.string.post_file)
    }

    /**
     * Use FormBody.Builder to build a request body that works like an HTML <form> tag.
     * Names and values will be encoded using an HTML-compatible form URL encoding.
     */
    private fun postFormParameters() {
        val formBody = FormBody.Builder()
            .add("search", "Jurassic Park")
            .build()

        executePostRequest("https://en.wikipedia.org/w/index.php", formBody)
        context?.showDoneToast(R.string.post_form_parameters)
    }

    /**
     * MultipartBody.Builder can build sophisticated request bodies compatible with HTML file upload forms.
     * Each part of a multipart request body is itself a request body, and can define its own headers.
     * If present, these headers should describe the part body, such as its Content-Disposition.
     * The Content-Length and Content-Type headers are added automatically if they’re available.
     */
    private fun postMutlipartRequest() {
        // Use the imgur image upload API as documented at https://api.imgur.com/endpoints/image
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("title", "Square Logo")
            .addFormDataPart(
                "image",
                "logo-square.png",
                writeOutBitmapIntoFile(File(activity?.filesDir, "logo-square.png")).asRequestBody(MEDIA_TYPE_PNG)
            )
            .build()

        val headers = mapOf("Authorization" to "Client-ID $IMGUR_CLIENT_ID")
        executePostRequest("https://api.imgur.com/3/image", requestBody, headers)
        context?.showDoneToast(R.string.post_multipart_request)
    }

    /**
     * Demonstrates a connection error which results in an exception. Span status is set to ERROR
     */
    private fun networkError() {
        val request = Request.Builder()
            .url("https://www.example.invalid")
            .build()

        runOnBackgroundThread {
            try {
                client.newCall(request).execute()
                context?.showDoneToast(R.string.network_error)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * To cache responses, you’ll need a cache directory that you can read and write to, and a limit on the cache’s size.
     * The cache directory should be private, and untrusted applications should not be able to read its contents!
     *
     * It is an error to have multiple caches accessing the same cache directory simultaneously.
     * Most applications should call new OkHttpClient() exactly once, configure it with
     * their cache, and use that same instance everywhere. Otherwise the two cache instances will
     * stomp on each other, corrupt the response cache, and possibly crash your program.
     *
     * Response caching uses HTTP headers for all configuration. You can add request headers like
     * Cache-Control: max-stale=3600 and OkHttp’s cache will honor them.
     * Your webserver configures how long responses are cached with its own response headers,
     * like Cache-Control: max-age=9600. There are cache headers to force a cached response,
     * force a network response, or force the network response to be validated with a conditional GET.
     */
    private fun responseCaching() {
        val request = Request.Builder()
            .url("https://publicobject.com/helloworld.txt")
            .build()

        runOnBackgroundThread {
            val response1Body = cachedClient.newCall(request).execute().use {
                if (!it.isSuccessful) throw IOException("Unexpected code $it")

                Log.v(TAG, "Response 1 response:          $it")
                Log.v(TAG, "Response 1 cache response:    ${it.cacheResponse}")
                Log.v(TAG, "Response 1 network response:  ${it.networkResponse}")
                return@use it.body?.string()
            }

            val response2Body = cachedClient.newCall(request).execute().use {
                if (!it.isSuccessful) throw IOException("Unexpected code $it")

                Log.v(TAG, "Response 2 response:          $it")
                Log.v(TAG, "Response 2 cache response:    ${it.cacheResponse}")
                Log.v(TAG, "Response 2 network response:  ${it.networkResponse}")
                return@use it.body?.string()
            }

            Log.v(TAG, "Response 2 equals Response 1? " + (response1Body == response2Body))
            context?.showDoneToast(R.string.response_caching)
        }
    }

    /**
     * Use Call.cancel() to stop an ongoing call immediately. If a thread is currently writing
     * a request or reading a response, it will receive an IOException.
     * Use this to conserve the network when a call is no longer necessary; for example when your
     * user navigates away from an application. Both synchronous and asynchronous calls can be canceled.
     */
    private fun canceledCall() {
        val request = Request.Builder()
            .url("https://httpbin.org/delay/2") // This URL is served with a 2 second delay.
            .build()

        val startNanos = System.nanoTime()
        val call = client.newCall(request)

        // Schedule a job to cancel the call in 1 second.
        executor.safeSchedule(1_000) {
            Log.v(TAG, String.format("%.2f Canceling call.%n", (System.nanoTime() - startNanos) / 1e9f))
            call.cancel()
            Log.v(TAG, String.format("%.2f Canceled call.%n", (System.nanoTime() - startNanos) / 1e9f))
        }

        Log.v(TAG, String.format("%.2f Executing call.%n", (System.nanoTime() - startNanos) / 1e9f))
        runOnBackgroundThread {
            try {
                call.execute().use { response ->
                    Log.e(
                        TAG,
                        String.format(
                            "%.2f Call was expected to fail, but completed: %s%n",
                            (System.nanoTime() - startNanos) / 1e9f,
                            response
                        )
                    )
                }
            } catch (e: IOException) {
                Log.v(
                    TAG,
                    String.format(
                        "%.2f Call failed as expected: %s%n",
                        (System.nanoTime() - startNanos) / 1e9f,
                        e
                    )
                )
                context?.showDoneToast(R.string.canceled_call)
            }
        }
    }

    /**
     * Called from other functions to perform synchronous GET to a given url.
     */
    private fun executeGetRequest(url: String, useRetryClient: Boolean = false) {
        val request = Request.Builder()
            .url(url)
            .build()

        val okHttpClient = if (useRetryClient) {
            retryClient
        } else {
            client
        }

        runOnBackgroundThread {
            try {
                okHttpClient.newCall(request).execute().use { response ->

                    for ((name, value) in response.headers) {
                        Log.v(TAG, "$name: $value")
                    }

                    Log.v(TAG, response.body?.string() ?: "null")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Called from other functions to perform asynchronous GET to a given url.
     */
    private fun executeAsynchronousGet(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }

                    Log.v(TAG, response.body?.string() ?: "null")
                }
            }
        })
    }

    /**
     * Called from other functions to perform POST to a given url with given request body and headers.
     */
    private fun executePostRequest(url: String, requestBody: RequestBody, headers: Map<String, String>? = null) {
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)

        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val request = requestBuilder.build()

        runOnBackgroundThread {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                Log.v(TAG, (response.body?.string() ?: "null"))
            }
        }
    }

    private fun writeOutBitmapIntoFile(file: File): File {
        val bitmap = createBitmap(100, 100)

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return file
    }

    companion object {
        private const val TAG = "OkHttp"
        private val MEDIA_TYPE_MARKDOWN = "text/x-markdown; charset=utf-8".toMediaType()

        private const val DISK_CACHE_SIZE = 10L * 1024L * 1024L // 10 MiB
        private const val DISK_CACHE_FOLDER = "http"

        /**
         * The imgur client ID for OkHttp recipes. If you're using imgur for anything other than running
         * these examples, please request your own client ID! https://api.imgur.com/oauth2
         */
        private const val IMGUR_CLIENT_ID = "9199fdef135c122"
        private val MEDIA_TYPE_PNG = "image/png".toMediaType()
    }
}
