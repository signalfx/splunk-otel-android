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
import org.gradle.api.logging.Logger

class MappingFileUploadClient(private val logger: Logger) {

    fun uploadMappingFile(
        mappingFile: File,
        buildId: String,
        applicationId: String,
        versionCode: Int,
        accessToken: String,
        realm: String
    ) {
        logger.info("Splunk RUM: Initiating HTTP upload")
        val url = buildUploadUrl(realm, applicationId, versionCode, buildId)
        logger.info("Splunk RUM: Upload URL: $url")
        logger.info("Splunk RUM: Using PUT method")
        logger.info("Splunk RUM: File size: ${mappingFile.length()} bytes")
        logger.debug("Splunk RUM: Application ID: $applicationId")
        logger.debug("Splunk RUM: Version Code: $versionCode")
        logger.debug("Splunk RUM: Build ID: $buildId")

        val connection = URL(url).openConnection() as HttpURLConnection
        logger.debug("Splunk RUM: Created HTTP connection")

        try {
            setupConnection(connection, accessToken)
            sendMultipartData(connection, mappingFile)
            handleResponse(connection)
        } catch (e: Exception) {
            logger.error("Splunk RUM: HTTP operation failed: ${e.message}")
            logger.debug("Splunk RUM: HTTP error details: ${e.stackTraceToString()}")
            throw e
        } finally {
            logger.debug("Splunk RUM: HTTP connection closed")
            connection.disconnect()
        }
    }

    private fun buildUploadUrl(realm: String, applicationId: String, versionCode: Int, buildId: String): String {
        val url = "https://api.$realm.signalfx.com/v2/rum-mfm/proguard/$applicationId/$versionCode/$buildId"
        logger.debug("Splunk RUM: Constructed upload URL for realm '$realm'")
        return url
    }

    private fun setupConnection(connection: HttpURLConnection, accessToken: String) {
        logger.debug("Splunk RUM: Configuring HTTP connection")

        connection.requestMethod = "PUT"
        connection.doOutput = true
        connection.doInput = true
        connection.setRequestProperty("X-SF-Token", accessToken)

        val boundary = "----Boundary${System.currentTimeMillis()}"
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.setRequestProperty("X-Boundary", boundary) // Store for later use
    }

    private fun sendMultipartData(connection: HttpURLConnection, mappingFile: File) {
        logger.info("Splunk RUM: Sending multipart data")
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
        logger.debug("Splunk RUM: Reading HTTP response")

        val responseCode = connection.responseCode
        logger.info("Splunk RUM: Response code: $responseCode")
        logger.info("Splunk RUM: Response message: ${connection.responseMessage}")

        if (responseCode in 200..299) {
            logger.debug("Splunk RUM: Success response received")
            val response = connection.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            logger.lifecycle("Splunk RUM: Upload successful (HTTP $responseCode)")
            if (response.isNotEmpty()) {
                logger.info("Splunk RUM: Response body: $response")
            } else {
                logger.debug("Splunk RUM: Empty response body")
            }
        } else {
            logger.warn("Splunk RUM: Error response received (HTTP $responseCode)")
            val errorResponse =
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
            logger.error("Splunk RUM: Error response body: $errorResponse")

            // Print response headers for debugging
            logger.debug("Splunk RUM: Response headers:")
            connection.headerFields.forEach { (key, values) ->
                logger.debug("Splunk RUM: Response header $key: ${values.joinToString(", ")}")
            }

            throw Exception("HTTP $responseCode: $errorResponse")
        }
    }
}
