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
import android.widget.Toast
import com.cisco.android.common.utils.extensions.safeSchedule
import com.cisco.android.common.utils.runOnBackgroundThread
import com.cisco.android.common.utils.runOnUiThread
import com.splunk.app.R
import com.splunk.app.databinding.FragmentOkhttpBinding
import com.splunk.app.ui.BaseFragment
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.navigation.extension.navigation
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class OkHttpFragment : BaseFragment<FragmentOkhttpBinding>() {

    private val client: OkHttpClient by lazy {
        OkHttpClient()
    }

    val retryInterceptor = Interceptor { chain: Interceptor.Chain ->
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

    private val retryClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .addInterceptor(
                retryInterceptor
            )
            .build()
    }

    private val cachedClient = OkHttpClient.Builder()
      .cache(Cache(File(activity?.cacheDir, DISK_CACHE_FOLDER), DISK_CACHE_SIZE))
      .build()

    private val executor = Executors.newScheduledThreadPool(1)

    override val titleRes: Int = R.string.okhttp_title

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentOkhttpBinding
        get() = FragmentOkhttpBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.synchronousGet.setOnClickListener { synchronousGet() }
        viewBinding.asynchronousGet.setOnClickListener { asynchronousGet() }
        viewBinding.concurrentAsynchronousGet.setOnClickListener { concurrentAsynchronousGet() }
        viewBinding.parentContextPropagationInAsyncGet.setOnClickListener { parentContextPropagationInAsyncGet() }
        viewBinding.unSuccessfulGet.setOnClickListener { unsuccessfulGet() }
        viewBinding.retryRequest.setOnClickListener { retryRequest() }
        viewBinding.multipleHeaders.setOnClickListener { multipleHeaders() }
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
    fun synchronousGet() {

        val request = Request.Builder()
            .url("https://publicobject.com/helloworld.txt")
            .build()

        runOnBackgroundThread {

            client.newCall(request).execute().use { response ->
               if (!response.isSuccessful) throw IOException("Unexpected code $response")

                for ((name, value) in response.headers) {
                    Log.v(TAG, "$name: $value")
                }

                Log.v(TAG, response.body?.string() ?: "null")
                showDoneToast("synchronousGet")
            }
        }
    }

    /**
     * Demonstrates an asynchronous successful okhttp GET.
     * Download a file on a worker thread, and get called back when the response is readable.
     * The callback is made after the response headers are ready.
     */
    fun asynchronousGet() {
        val url = "https://httpbin.org/robots.txt"
        performAsynchronousGet(url)
        showDoneToast("asynchronousGet")
    }

    /**
     * Demonstrates multiple successful asynchronous okhttp GET operations to the same url in parallel.
     */
    fun concurrentAsynchronousGet() {
        val url = "https://httpbin.org/headers"
        performAsynchronousGet(url)
        performAsynchronousGet(url)
        showDoneToast("concurrentAsynchronousGet")
    }

    /**
     * Called from other functions to perform asynchronous GET to a given url.
     */
    fun performAsynchronousGet(url: String) {
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
     * Demonstrates correct context propagation for asynchronous requests when parent context exists.
     */

    fun parentContextPropagationInAsyncGet() {

        val lock = CountDownLatch(1)

        val openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder().build())
            .build()

        val span = openTelemetry.getTracer("Test Tracer").spanBuilder("A Span").startSpan()

        span!!.makeCurrent().use { ignored ->

            val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(
                    Interceptor { chain: Interceptor.Chain ->
                        val currentSpan =
                            Span.current().spanContext
                        // Verify context propagation.
                        if (span.spanContext.traceId ==
                            currentSpan.traceId) {
                            Log.d( TAG, "Testing parent context propagation in async get - trace id's are same as expected.")
                        } else {
                            Log.e( TAG, "Testing parent context propagation in async get - trace id's are unexpectedly not same.")
                        }
                        chain.proceed(chain.request())
                    })
                .build()

            val request = Request.Builder()
                .url("https://publicobject.com/helloworld.txt")
                .build()

            client.newCall(request).enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(
                        call: Call, response: Response
                    ) {
                        // Verify that the original caller's context is the current one here.
                        if (span == Span.current()) {
                            Log.d( TAG, "Testing parent context propagation in async get - contexts are same as expected.")
                        } else {
                            Log.e( TAG, "Testing parent context propagation in async get - Contexts are unexpectedly different.")
                        }
                        lock.countDown()
                    }
                })
        }
        lock.await()
        span.end()
        showDoneToast("parentContextPropagationInAsyncGet")
    }

    /**
     * Demonstrates an unsuccessful okhttp GET.
     */
    fun unsuccessfulGet() {
        val request = Request.Builder()
            .url("http://httpbin.org/status/404")
            .build()

        runOnBackgroundThread {
            try {
                client.newCall(request).execute().use { response ->

                    for ((name, value) in response.headers) {
                        Log.v(TAG, "$name: $value")
                    }

                    Log.v(TAG, response.body?.string() ?: "null")

                    showDoneToast("unsuccessfulGet")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Demonstrates a request i.e being retried two additional times.
     * http.request.resend_count is being set on each retried request.
     */
    fun retryRequest() {
        val request = Request.Builder()
            .url("https://httpbin.org/status/503")
            .build()

        runOnBackgroundThread {
            try {
                retryClient.newCall(request).execute().use { response ->

                    for ((name, value) in response.headers) {
                        Log.v(TAG, "$name: $value")
                    }

                    Log.v(TAG, response.body?.string() ?: "null")

                    showDoneToast("retryRequest")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Typically HTTP headers work like a Map<String, String>: each field has one value or none.
     * But some headers permit multiple values, like Guava’s Multimap.
     * For example, it’s legal and common for an HTTP response to supply multiple Vary headers.
     */
    fun multipleHeaders() {
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
                showDoneToast("multipleHeaders")
            }
        }
    }

    /**
     * Use an HTTP POST to send a request body to a service.
     * This example posts a markdown document to a web service that renders markdown as HTML.
     */
    fun postMarkdown() {
        val postBody = """
        |Releases
        |--------
        |
        | * _1.0_ May 6, 2013
        | * _1.1_ June 15, 2013
        | * _1.2_ August 11, 2013
        |""".trimMargin()

        val request = Request.Builder()
            .url("https://api.github.com/markdown/raw")
            .post(postBody.toRequestBody(MEDIA_TYPE_MARKDOWN))
            .build()

        runOnBackgroundThread {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                Log.v(TAG, (response.body?.string() ?: "null"))
                showDoneToast("postMarkdown")
            }
        }
    }

    /**
     * Here we POST a request body as a stream. The content of this request body is being generated
     * as it’s being written. This example streams directly into the Okio buffered sink.
     */
    fun postStreaming() {
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

        val request = Request.Builder()
            .url("https://api.github.com/markdown/raw")
            .post(requestBody)
            .build()

        runOnBackgroundThread {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                Log.v(TAG, response.body?.string() ?: "null")
                showDoneToast("postStreaming")
            }
        }
    }

    /**
     * It’s easy to use a file as a request body.
     */
    fun postFile() {
        val file = File(activity?.filesDir, "README.md")

        file.writeText(
            """
            |Releases
            |--------
            |
            | * _1.0_ May 6, 2013
            | * _1.1_ June 15, 2013
            | * _1.2_ August 11, 2013
            |""".trimMargin()
        )

        val request = Request.Builder()
            .url("https://api.github.com/markdown/raw")
            .post(file.asRequestBody(MEDIA_TYPE_MARKDOWN))
            .build()

        runOnBackgroundThread {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                Log.v(TAG, response.body?.string() ?: "null")
                showDoneToast("postFile")
            }
        }
    }

    /**
     * Use FormBody.Builder to build a request body that works like an HTML <form> tag.
     * Names and values will be encoded using an HTML-compatible form URL encoding.
     */
    fun postFormParameters() {
        val formBody = FormBody.Builder()
            .add("search", "Jurassic Park")
            .build()
        val request = Request.Builder()
            .url("https://en.wikipedia.org/w/index.php")
            .post(formBody)
            .build()

        runOnBackgroundThread {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                Log.v(TAG, response.body?.string() ?: "null")
                showDoneToast("postFormParameters")
            }
        }
    }

    /**
     * MultipartBody.Builder can build sophisticated request bodies compatible with HTML file upload forms.
     * Each part of a multipart request body is itself a request body, and can define its own headers.
     * If present, these headers should describe the part body, such as its Content-Disposition.
     * The Content-Length and Content-Type headers are added automatically if they’re available.
     */
    fun postMutlipartRequest() {
        // Use the imgur image upload API as documented at https://api.imgur.com/endpoints/image
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("title", "Square Logo")
            .addFormDataPart(
                "image", "logo-square.png",
                writeOutBitmapIntoFile(File(activity?.filesDir, "logo-square.png")).asRequestBody(MEDIA_TYPE_PNG)
            )
            .build()

        val request = Request.Builder()
            .header("Authorization", "Client-ID $IMGUR_CLIENT_ID")
            .url("https://api.imgur.com/3/image")
            .post(requestBody)
            .build()

        runOnBackgroundThread {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                Log.v(TAG, response.body?.string() ?: "null")
                showDoneToast("postMutlipartRequest")
            }
        }
    }

    /**
     * Demonstrates a connection error which results in an exception. Span status is set to ERROR
     */
    fun networkError() {
        val request = Request.Builder()
            .url("https://www.example.invalid")
            .build()

        runOnBackgroundThread {
            try {
                client.newCall(request).execute()
                showDoneToast("networkError")
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
    fun responseCaching() {
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
            showDoneToast("responseCaching")
        }
    }

    /**
     * Use Call.cancel() to stop an ongoing call immediately. If a thread is currently writing
     * a request or reading a response, it will receive an IOException.
     * Use this to conserve the network when a call is no longer necessary; for example when your
     * user navigates away from an application. Both synchronous and asynchronous calls can be canceled.
     */
    fun canceledCall() {
        val request = Request.Builder()
            .url("http://httpbin.org/delay/2") // This URL is served with a 2 second delay.
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
                            (System.nanoTime() - startNanos) / 1e9f, response
                        )
                    )
                }
            } catch (e: IOException) {
                Log.v(
                    TAG,
                    String.format(
                        "%.2f Call failed as expected: %s%n",
                        (System.nanoTime() - startNanos) / 1e9f, e
                    )
                )
                showDoneToast("canceledCall")
            }
        }
    }

    private fun showDoneToast(methodName: String) {
        runOnUiThread {
            Toast.makeText(context, getString(R.string.http_toast, methodName), Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeOutBitmapIntoFile(file: File): File {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

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
