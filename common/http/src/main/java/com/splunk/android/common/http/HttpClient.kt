package com.splunk.android.common.http

import com.splunk.android.common.http.extension.toHeaders
import com.splunk.android.common.http.extension.toURL
import com.splunk.android.common.http.extension.write
import com.splunk.android.common.http.model.Header
import com.splunk.android.common.http.model.Query
import com.splunk.android.common.http.model.Response
import com.splunk.android.common.http.model.part.Part
import com.splunk.sdk.common.utils.extensions.safeSubmit
import com.splunk.sdk.common.utils.thread.NamedThreadFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.util.UUID
import java.util.concurrent.Executors

class HttpClient {

    private val executor = Executors.newSingleThreadExecutor(NamedThreadFactory("HttpClient"))

    fun makeGetRequest(url: String, queries: List<Query>, headers: List<Header>, callback: Callback) {
        executor.safeSubmit {
            makeRequest(url, "GET", queries, headers, callback)
        }
    }

    fun makePostRequest(url: String, queries: List<Query>, headers: List<Header>, body: String, callback: Callback) {
        makePostRequest(url, queries, headers, body.toByteArray(), callback)
    }

    fun makePostRequest(url: String, queries: List<Query>, headers: List<Header>, body: ByteArray, callback: Callback) {
        executor.safeSubmit {
            val finalHeaders = headers + Header("Content-Length", body.size.toString())

            makeRequest(url, "POST", queries, finalHeaders, callback) {
                it.write(body)
            }
        }
    }

    fun makePostRequest(url: String, queries: List<Query>, headers: List<Header>, body: File, callback: Callback) {
        executor.safeSubmit {
            val finalHeaders = headers + Header("Content-Length", body.length().toString())

            makeRequest(url, "POST", queries, finalHeaders, callback) {
                body.inputStream().buffered().copyTo(it)
            }
        }
    }

    fun makePostRequest(url: String, queries: List<Query>, headers: List<Header>, bodyParts: List<Part>, callback: Callback) {
        executor.safeSubmit {
            val boundary = UUID.randomUUID().toString()

            val stream = ByteArrayOutputStream()
            try {
                stream.write(bodyParts, boundary)
            } catch (e: FileNotFoundException) {
                callback.onFailed(e)
                return@safeSubmit
            }

            val finalHeaders = headers + listOf(
                Header("Content-Type", "multipart/form-data; boundary=$boundary"),
                Header("Content-Length", stream.size().toString())
            )

            makeRequest(url, "POST", queries, finalHeaders, callback) {
                stream.writeTo(it)
            }
        }
    }

    private fun makeRequest(url: String, method: String, queries: List<Query>, headers: List<Header>, callback: Callback, onOutput: ((OutputStream) -> Unit)? = null) {
        val connection = url.toURL(queries).openConnection() as HttpURLConnection

        for (header in headers)
            connection.setRequestProperty(header.name, header.value)

        connection.connectTimeout = TIMEOUT
        connection.readTimeout = TIMEOUT
        connection.requestMethod = method
        connection.doOutput = onOutput != null
        connection.doInput = true

        val response = try {
            if (onOutput != null) {
                val outputStream = connection.outputStream.buffered()
                onOutput(outputStream)
                outputStream.flush()
            }

            val body = when (connection.responseCode) {
                204 -> byteArrayOf()
                200 -> connection.inputStream.buffered().readBytes()
                in 0..400 -> connection.errorStream.buffered().readBytes()
                else -> connection.errorStream.buffered().readBytes()
            }

            Response(
                code = connection.responseCode,
                headers = connection.headerFields.toHeaders(),
                body = body
            )
        } catch (e: Exception) {
            callback.onFailed(e)
            return
        }

        callback.onSuccess(response)
    }

    interface Callback {
        fun onSuccess(response: Response)
        fun onFailed(e: Exception)
    }

    private companion object {
        const val TIMEOUT = 15_000
    }
}
