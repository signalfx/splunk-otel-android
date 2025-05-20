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

package com.splunk.sdk.interception.okhttp

import com.splunk.rum.integration.agent.api.network.SplunkNetworkRequest
import com.splunk.rum.integration.okhttp.InterceptionManager
import com.splunk.rum.integration.okhttp.interceptor.SplunkHeadersInterceptor
import com.splunk.rum.integration.okhttp.interceptor.SplunkMaskUrlInterceptor
import com.splunk.rum.integration.okhttp.interceptor.SplunkOkHttpInterceptor
import com.splunk.rum.integration.okhttp.listener.OkHttpConnectorListenerDummy
import com.splunk.rum.integration.okhttp.model.Mask
import com.splunk.rum.integration.okhttp.model.SplunkChain
import java.net.URL
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class InterceptionManagerTest {

    private lateinit var interceptionManager: InterceptionManager
    private lateinit var sdkConnector: OkHttpConnectorListenerDummy
    private lateinit var mockURL: URL
    private lateinit var mockWebServer: MockWebServer
    private lateinit var httpClientBuilder: OkHttpClient.Builder
    private var httpClient: OkHttpClient? = null
    private val splunkOkHttpInterceptors = mutableListOf<SplunkOkHttpInterceptor>()

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mockURL = URL(mockWebServer.url("").toUrl(), "/test/")

        httpClientBuilder = OkHttpClient.Builder()
        sdkConnector = OkHttpConnectorListenerDummy()
        interceptionManager = InterceptionManager(sdkConnector)
    }

    @After
    fun destroy() {
        splunkOkHttpInterceptors.forEach {
            interceptionManager.getOkHttpInterceptors(httpClientBuilder).remove(it)
        }
        splunkOkHttpInterceptors.clear()

        httpClient = null
        mockWebServer.shutdown()
    }

    @Test
    fun `basic GET`() {
        val request = TestRequest(
            url = mockURL,
            method = GET_METHOD,
            mediaType = null,
            body = null,
            headers = emptyMap()
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = null,
            body = null,
            headers = emptyMap()
        )

        whenDefaultInterceptorAdded()
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenStatusCodeIs(OK_STATUS_CODE)
        thenInterceptedUrlIs(mockURL)
        thenInterceptedMethodIs(GET_METHOD)
    }

    @Test
    fun `mask sensitive parts of URL`() {
        val request = TestRequest(
            url = URL(mockURL.toString() + "sample?secret=token&test=param"),
            method = GET_METHOD,
            mediaType = null,
            body = null,
            headers = emptyMap()
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = null,
            body = null,
            headers = emptyMap()
        )

        val masks = listOf(
            Mask(
                regexPattern = "(secret=)[^&]+(&*)",
                replaceWith = "$1<hidden>$2"
            )
        )

        whenSplunkMaskUrlInterceptorAdded(masks)
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenInterceptedUrlIs(URL(mockURL.toString() + "sample?secret=<hidden>&test=param"))
    }

    @Test
    fun `allowed headers interception`() {
        val request = TestRequest(
            url = mockURL,
            method = GET_METHOD,
            mediaType = null,
            body = null,
            headers = mapOf("date" to "test_date")
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = null,
            body = null,
            headers = mapOf("date" to "test_date")
        )

        whenDefaultInterceptorAdded()
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenRequestContainsHeaderWithValue("date", "test_date")
        thenResponseContainsHeaderWithValue("date", "test_date")
    }

    @Test
    fun `multiple allowed headers interception`() {
        val request = TestRequest(
            url = mockURL,
            method = GET_METHOD,
            mediaType = null,
            body = null,
            headers = mapOf("date" to "test_date_a", "Date" to "test_date_b")
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = null,
            body = null,
            headers = mapOf("Date" to "test_date_a", "date" to "test_date_b")
        )

        whenDefaultInterceptorAdded()
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenRequestContainsHeaderWithValue("date", "test_date_a")
        thenRequestContainsHeaderWithValue("date", "test_date_b")
        thenResponseContainsHeaderWithValue("date", "test_date_a")
        thenRequestContainsHeaderWithValue("date", "test_date_b")
    }

    @Test
    fun `not allowed headers interception`() {
        val request = TestRequest(
            url = mockURL,
            method = GET_METHOD,
            mediaType = null,
            body = null,
            headers = mapOf("NotAllowed" to "test")
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = null,
            body = null,
            headers = mapOf("NotAllowed" to "test")
        )

        whenDefaultInterceptorAdded()
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenRequestDoesNotContainHeader("not-allowed")
        thenResponseDoesNotContainHeader("not-allowed")
    }

    @Test
    fun `headers interception with SplunkHeadersInterceptor`() {
        val request = TestRequest(
            url = mockURL,
            method = GET_METHOD,
            mediaType = null,
            body = null,
            headers = mapOf("allowed_test" to "test", "restricted_test" to "test")
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = null,
            body = null,
            headers = mapOf("allowed_a" to "test", "allowed_b" to "test", "restricted" to "test")
        )

        val allowedHeaders = setOf("allowed.*")

        whenSplunkHeadersInterceptorAdded(allowedHeaders)
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenRequestContainsHeaderWithValue("allowed_test", "test")
        thenRequestDoesNotContainHeader("restricted_test")
        thenResponseContainsHeaderWithValue("allowed_a", "test")
        thenResponseContainsHeaderWithValue("allowed_b", "test")
        thenResponseDoesNotContainHeader("restricted")
    }

    /*@Test
    fun `json request and response body with default interceptor`() {
        val request = TestRequest(
            url = mockURL,
            method = POST_METHOD,
            mediaType = jsonMediaType,
            body = JSONObject().put("request", "body").toString(),
            headers = mapOf()
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = jsonMediaType,
            body = JSONObject().put("response", "body").toString(),
            headers = mapOf()
        )

        whenDefaultInterceptorAdded()
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenRequestBodyIsNull()
        thenRequestBodyIsNull()
    }*/

    /*@Test
    fun `json request and response body with NonBinaryBodyInterceptor interceptor`() {
        val request = TestRequest(
            url = mockURL,
            method = POST_METHOD,
            mediaType = jsonMediaType,
            body = JSONObject().put("request", "body").toString(),
            headers = mapOf()
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = jsonMediaType,
            body = JSONObject().put("response", "body").toString(),
            headers = mapOf()
        )

        whenNonBinaryBodyInterceptorAdded()
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenRequestBodyIs(JSONObject().put("request", "body").toString())
        thenResponseBodyIs(JSONObject().put("response", "body").toString())
    }*/

    /*@Test
    fun `json request and response body masking with SplunkMaskBodyInterceptor`() {
        val request = TestRequest(
            url = mockURL,
            method = POST_METHOD,
            mediaType = jsonMediaType,
            body = JSONObject()
                .put("request", "simple_request")
                .put("request_alternative", "complex_request")
                .toString(),
            headers = mapOf()
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = jsonMediaType,
            body = JSONObject()
                .put("response", "simple_response")
                .put("response_alternative", "complex_response")
                .toString(),
            headers = mapOf()
        )

        val bodyMasks = listOf(
            Mask(
                regexPattern = "\"request\":\\s*\"[^\\/\"]+\"",
                replaceWith = "\"request\": <hidden>"
            ),
            Mask(
                regexPattern = "\"response(\\w*)\":\\s*\"[^\\/\"]+\"",
                replaceWith = "\"response$1\": <hidden>"
            ),
        )

        whenNonBinaryBodyInterceptorAdded()
        whenSplunkMaskBodyInterceptor(bodyMasks)
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenRequestBodyDoesNotContain("simple_request")
        thenRequestBodyContains("complex_request")
        thenResponseBodyDoesNotContain("simple_response")
        thenResponseBodyDoesNotContain("complex_response")
        thenResponseBodyContains("response_alternative")
    }*/

    @Test
    fun `failed request`() {
        val request = TestRequest(
            url = mockURL,
            method = POST_METHOD,
            mediaType = jsonMediaType,
            body = JSONObject().put("request", "body").toString(),
            headers = mapOf()
        )

        whenDefaultInterceptorAdded()
        whenOkHttpBuilt()
        whenFailedCallExecuted(request)

        thenStatusCodeIs(UNKNOWN_STATUS_CODE)
    }

    @Test
    fun `not allowed headers with allow all headers interceptor`() {
        val request = TestRequest(
            url = mockURL,
            method = GET_METHOD,
            mediaType = null,
            body = null,
            headers = mapOf("Not-Allowed" to "test")
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = null,
            body = null,
            headers = mapOf("not-allowed" to "test")
        )

        whenAllowAllHeadersInterceptorAdded()
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenRequestContainsHeaderWithValue("not-allowed", "test")
        thenResponseContainsHeaderWithValue("not-allowed", "test")
    }

    /*@Test
    fun `binary response body`() {
        val request = TestRequest(
            url = mockURL,
            method = GET_METHOD,
            mediaType = null,
            body = null,
            headers = mapOf()
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = binaryMediaType,
            body = Buffer().write(ByteArray(4096)),
            headers = mapOf()
        )

        whenDefaultInterceptorAdded()
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenResponseBodyIs(null)
    }*/

    /*@Test
    fun `binary body with allow all content types interceptor`() {
        val responseBodyLength = 4096

        val request = TestRequest(
            url = mockURL,
            method = GET_METHOD,
            mediaType = null,
            body = null,
            headers = mapOf()
        )

        val response = TestResponse(
            statusCode = OK_STATUS_CODE,
            mediaType = binaryMediaType,
            body = Buffer().write(ByteArray(responseBodyLength)),
            headers = mapOf()
        )

        whenAllowAllContentTypesInterceptorAdded()
        whenOkHttpBuilt()
        whenSuccessfulCallExecuted(request, response)

        thenResponseBodyLengthIs(responseBodyLength)
    }*/

    /**
     * WARNING: Must be called before whenOkHttpBuilt().
     */
    private fun whenInterceptorAdded(okHttpInterceptor: SplunkOkHttpInterceptor) {
        splunkOkHttpInterceptors.add(okHttpInterceptor)
        interceptionManager.getOkHttpInterceptors(httpClientBuilder).add(okHttpInterceptor)
    }

    private fun whenDefaultInterceptorAdded() {
        whenInterceptorAdded(DefaultOkHttpInterceptor())
    }

    /*private fun whenNonBinaryBodyInterceptorAdded() {
        whenInterceptorAdded(SplunkNonBinaryBodyInterceptor())
    }*/

    private fun whenAllowAllHeadersInterceptorAdded() {
        whenInterceptorAdded(AllowAllHeadersInterceptor())
    }

    /*private fun whenAllowAllContentTypesInterceptorAdded() {
        whenInterceptorAdded(AllowAllContentTypesInterceptor())
    }*/

    /*private fun whenSplunkMaskBodyInterceptor(masks: List<Mask>) {
        whenInterceptorAdded(SplunkMaskBodyInterceptor(masks))
    }*/

    private fun whenSplunkHeadersInterceptorAdded(allowedHeaders: Set<String>) {
        whenInterceptorAdded(SplunkHeadersInterceptor(allowedHeaders))
    }

    private fun whenSplunkMaskUrlInterceptorAdded(masks: List<Mask>) {
        whenInterceptorAdded(SplunkMaskUrlInterceptor(masks))
    }

    private fun whenOkHttpBuilt() {
        if (httpClient == null) {
            httpClient = httpClientBuilder.build()
        }
    }

    /**
     * WARNING: Must be called after whenOkHttpBuilt().
     */
    private fun whenSuccessfulCallExecuted(
        testRequest: TestRequest,
        testResponse: TestResponse
    ) {
        val request = buildRequest(testRequest)
        val response = buildResponse(testResponse)

        mockWebServer.enqueue(response)
        httpClient?.newCall(request)?.execute()
    }

    /**
     * WARNING: Must be called after whenOkHttpBuilt().
     */
    private fun whenFailedCallExecuted(testRequest: TestRequest) {
        val request = buildRequest(testRequest)

        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        try {
            httpClient?.newCall(request)?.execute()
        } catch (_: Exception) {
        }
    }

    private fun thenStatusCodeIs(statusCode: Int) {
        Assert.assertEquals(statusCode, sdkConnector.request?.statusCode)
    }

    private fun thenInterceptedMethodIs(method: String) {
        Assert.assertEquals(method, sdkConnector.request?.method)
    }

    private fun thenInterceptedUrlIs(mockURL: URL) {
        Assert.assertEquals(mockURL, sdkConnector.request?.url)
    }

    /*private fun thenRequestBodyIs(body: String?) {
        Assert.assertEquals(body, sdkConnector.request?.requestBody)
    }*/

    /*private fun thenResponseBodyIs(body: String?) {
        Assert.assertEquals(body, sdkConnector.request?.responseBody)
    }*/

    /*private fun thenRequestBodyIsNull() {
        Assert.assertNull(sdkConnector.request?.requestBody)
    }*/

    /*private fun thenResponseBodyLengthIs(responseBodyLength: Int) {
        Assert.assertEquals(responseBodyLength, sdkConnector.request?.responseBody?.length ?: -1)
    }*/

    /*private fun thenRequestBodyContains(contains: String) {
        Assert.assertTrue(sdkConnector.request?.requestBody?.contains(contains) == true)
    }*/

    /*private fun thenResponseBodyContains(contains: String) {
        Assert.assertTrue(sdkConnector.request?.responseBody?.contains(contains) == true)
    }*/

    /*private fun thenRequestBodyDoesNotContain(contains: String) {
        Assert.assertFalse(sdkConnector.request?.requestBody?.contains(contains) == true)
    }*/

    /*private fun thenResponseBodyDoesNotContain(contains: String) {
        Assert.assertFalse(sdkConnector.request?.responseBody?.contains(contains) == true)
    }*/

    private fun thenRequestContainsHeaderWithValue(key: String, value: String) {
        Assert.assertTrue(sdkConnector.request?.requestHeaders?.contains(key) == true)
        Assert.assertTrue(sdkConnector.request?.requestHeaders?.get(key)?.contains(value) == true)
    }

    private fun thenResponseContainsHeaderWithValue(key: String, value: String) {
        Assert.assertTrue(sdkConnector.request?.responseHeaders?.contains(key) == true)
        Assert.assertTrue(sdkConnector.request?.responseHeaders?.get(key)?.contains(value) == true)
    }

    private fun thenRequestDoesNotContainHeader(key: String) {
        Assert.assertTrue(sdkConnector.request?.requestHeaders?.contains(key) == false)
    }

    private fun thenResponseDoesNotContainHeader(key: String) {
        Assert.assertTrue(sdkConnector.request?.responseHeaders?.contains(key) == false)
    }

    private fun buildRequest(testRequest: TestRequest): Request = Request.Builder()
        .url(testRequest.url)
        .method(testRequest.method, testRequest.body?.toRequestBody(testRequest.mediaType))
        .also { builder ->
            testRequest.headers.forEach {
                builder.addHeader(it.key, it.value)
            }
        }
        .build()

    private fun buildResponse(testResponse: TestResponse): MockResponse = MockResponse()
        .setResponseCode(testResponse.statusCode)
        .also { response ->
            if (testResponse.mediaType != null) {
                response.addHeader("Content-Type", testResponse.mediaType.toString())
            }
            if (testResponse.body != null) {
                response.setBody(testResponse.body)
            }
            testResponse.headers.forEach {
                response.addHeader(it.key, it.value)
            }
        }

    data class TestRequest(
        val url: URL,
        val method: String,
        val mediaType: MediaType?,
        val body: String?,
        val headers: Map<String, String>
    )

    data class TestResponse(
        val statusCode: Int,
        val mediaType: MediaType?,
        val body: Buffer?,
        val headers: Map<String, String>
    ) {
        constructor(statusCode: Int, mediaType: MediaType?, body: String, headers: Map<String, String>) : this(statusCode, mediaType, body.run { Buffer().writeUtf8(body) }, headers)
    }

    class DefaultOkHttpInterceptor : SplunkOkHttpInterceptor {
        override fun onIntercept(original: SplunkChain, intercepted: SplunkNetworkRequest): SplunkNetworkRequest = intercepted
    }

    class AllowAllHeadersInterceptor : SplunkOkHttpInterceptor {
        override fun onIntercept(original: SplunkChain, intercepted: SplunkNetworkRequest): SplunkNetworkRequest {
            intercepted.requestHeaders = original.request.headers.parse()
            intercepted.responseHeaders = original.response?.headers?.parse()

            return intercepted
        }

        private fun Headers.parse(): MutableMap<String, MutableList<String>> {
            val parsedHeaders = mutableMapOf<String, MutableList<String>>()

            this.forEach { header ->
                val name = header.first.lowercase()
                val value = header.second
                if (name in parsedHeaders) {
                    parsedHeaders[name]?.add(value)
                } else {
                    parsedHeaders[name] = mutableListOf(value)
                }
            }

            return parsedHeaders
        }
    }

    private companion object {
        const val UNKNOWN_STATUS_CODE = -1
        const val OK_STATUS_CODE = 200
        const val GET_METHOD = "GET"
        const val POST_METHOD = "POST"

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    }
}
