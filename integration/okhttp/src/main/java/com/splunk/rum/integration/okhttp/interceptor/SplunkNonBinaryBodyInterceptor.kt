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

import com.splunk.rum.integration.agent.api.network.SplunkNetworkRequest
import com.splunk.rum.integration.okhttp.model.SplunkChain
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset

// Not supported by a BE at the moment
/*class SplunkNonBinaryBodyInterceptor : SplunkOkHttpInterceptor {

    override fun onIntercept(original: SplunkChain, intercepted: SplunkNetworkRequest): SplunkNetworkRequest {
        intercepted.requestBody = original.request.parseBody()
        intercepted.responseBody = original.response?.parseBody()

        return intercepted
    }

    private fun Request.parseBody(): String? {
        val body = body ?: return null
        val contentType = body.contentType() ?: return null
        val fullType = contentType.toFullType().lowercase()
        val charset = contentType.toCharset() ?: Charsets.UTF_8

        return if (fullType in nonBinaryContentTypes) {
            try {
                val buffer = Buffer()
                body.writeTo(buffer)

                buffer.readString(body.contentLength(), charset)
            } catch (e: IOException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        } else
            null
    }

    private fun Response.parseBody(): String? {
        val body = body ?: return null
        val contentType = body.contentType() ?: return null
        val fullType = contentType.toFullType().lowercase()

        return if (fullType in nonBinaryContentTypes) {
            try {
                peekBody(MAX_CONTENT_LENGTH).string()
            } catch (e: IOException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        } else
            null
    }

    private fun MediaType.toFullType(): String {
        return "${type}/${subtype}"
    }

    private fun MediaType.toCharset(): Charset? {
        return charset(null)
    }

    private companion object {

        const val MAX_CONTENT_LENGTH = 1_000_000L

        val nonBinaryContentTypes = setOf(
            "text/plain",
            "text/csv",
            "application/xml",
            "text/xml",
            "application/json",
            "application/ld+json",
            "text/x-markdown"
        )
    }
}*/
