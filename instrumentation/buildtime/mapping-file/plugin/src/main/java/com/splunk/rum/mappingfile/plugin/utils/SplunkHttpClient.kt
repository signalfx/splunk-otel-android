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

package com.splunk.rum.mappingfile.plugin.utils

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class SplunkHttpClient {

    fun uploadMappingFile(
        mappingFile: File,
        buildId: String,
        applicationId: String,
        versionCode: Int,
        accessToken: String,
        realm: String
    ) {
        val url = buildUploadUrl(realm, applicationId, versionCode, buildId)
        println("Splunk RUM: Upload URL: $url")
        println("Splunk RUM: Using PUT method")
        println("Splunk RUM: File size: ${mappingFile.length()} bytes")

        val connection = URL(url).openConnection() as HttpURLConnection

        try {
            setupConnection(connection, accessToken)
            sendMultipartData(connection, mappingFile)
            handleResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUploadUrl(realm: String, applicationId: String, versionCode: Int, buildId: String): String {
        return "https://api.$realm.signalfx.com/v2/rum-mfm/proguard/$applicationId/$versionCode/$buildId"
    }

    private fun setupConnection(connection: HttpURLConnection, accessToken: String) {
        connection.requestMethod = "PUT"
        connection.doOutput = true
        connection.doInput = true
        connection.setRequestProperty("X-SF-Token", accessToken)

        val boundary = "----Boundary${System.currentTimeMillis()}"
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.setRequestProperty("X-Boundary", boundary) // Store for later use
    }

    private fun sendMultipartData(connection: HttpURLConnection, mappingFile: File) {
        val boundary = connection.getRequestProperty("X-Boundary")

        connection.outputStream.use { outputStream ->
            val writer = java.io.PrintWriter(java.io.OutputStreamWriter(outputStream, "UTF-8"), true)

            // Add filename parameter
            writer.append("--$boundary").append("\r\n")
            writer.append("Content-Disposition: form-data; name=\"filename\"").append("\r\n")
            writer.append("\r\n")
            writer.append(mappingFile.name).append("\r\n")
            writer.flush()

            // Add file
            writer.append("--$boundary").append("\r\n")
            writer.append(
                "Content-Disposition: form-data; name=\"file\"; filename=\"${mappingFile.name}\""
            ).append("\r\n")
            writer.append("Content-Type: application/octet-stream").append("\r\n")
            writer.append("\r\n")
            writer.flush()

            mappingFile.inputStream().use { it.copyTo(outputStream) }
            outputStream.flush()

            writer.append("\r\n--$boundary--\r\n")
            writer.close()
        }
    }

    private fun handleResponse(connection: HttpURLConnection) {
        val responseCode = connection.responseCode
        println("Splunk RUM: Response code: $responseCode")
        println("Splunk RUM: Response message: ${connection.responseMessage}")

        if (responseCode in 200..299) {
            val response = connection.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            println("Splunk RUM: Upload successful (HTTP $responseCode)")
            if (response.isNotEmpty()) {
                println("Splunk RUM: Response body: $response")
            }
        } else {
            val errorResponse =
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
            println("Splunk RUM: Error response body: $errorResponse")

            // Print response headers for debugging
            connection.headerFields.forEach { (key, values) ->
                println("Splunk RUM: Response header $key: ${values.joinToString(", ")}")
            }

            throw Exception("HTTP $responseCode: $errorResponse")
        }
    }
}