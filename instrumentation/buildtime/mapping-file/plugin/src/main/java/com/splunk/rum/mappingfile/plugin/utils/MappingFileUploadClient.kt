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
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MappingFileUploadClient(private val logger: SplunkLogger) {

    companion object {
        private const val CRLF = "\r\n"
    }

    fun uploadMappingFile(
        mappingFile: File,
        buildId: String,
        applicationId: String,
        versionCode: Int,
        accessToken: String,
        realm: String
    ) {
        logger.info("Upload", "Initiating HTTP upload")
        val url = buildUploadUrl(realm, applicationId, versionCode, buildId)
        logger.info("Upload", "Upload URL: $url")
        logger.info("Upload", "Using PUT method")
        logger.info("Upload", "File size: ${mappingFile.length()} bytes")
        logger.debug("Upload", "Application ID: $applicationId")
        logger.debug("Upload", "Version Code: $versionCode")
        logger.debug("Upload", "Build ID: $buildId")

        val connection = URL(url).openConnection() as HttpURLConnection
        logger.debug("Upload", "Created HTTP connection")

        try {
            setupConnection(connection, accessToken)
            sendMultipartData(connection, mappingFile)
            handleResponse(connection, realm, applicationId, versionCode, buildId, mappingFile)
        } finally {
            logger.debug("Upload", "HTTP connection closed")
            connection.disconnect()
        }
    }

    private fun buildUploadUrl(realm: String, applicationId: String, versionCode: Int, buildId: String): String {
        val url = "https://api.$realm.signalfx.com/v2/rum-mfm/proguard/$applicationId/$versionCode/$buildId"
        logger.debug("Upload", "Constructed upload URL for realm '$realm'")
        return url
    }

    private fun setupConnection(connection: HttpURLConnection, accessToken: String) {
        logger.debug("Upload", "Configuring HTTP connection")

        connection.requestMethod = "PUT"
        connection.doOutput = true
        connection.doInput = true
        connection.setRequestProperty("X-SF-Token", accessToken)

        val boundary = "----Boundary${System.currentTimeMillis()}"
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.setRequestProperty("X-Boundary", boundary)
    }

    private fun sendMultipartData(connection: HttpURLConnection, mappingFile: File) {
        logger.info("Upload", "Sending multipart data")
        val boundary = connection.getRequestProperty("X-Boundary")

        connection.outputStream.use { outputStream ->
            val writer = java.io.PrintWriter(java.io.OutputStreamWriter(outputStream, "UTF-8"), true)

            // Add filename parameter
            writer.append("--$boundary$CRLF")
            writer.append("Content-Disposition: form-data; name=\"filename\"$CRLF")
            writer.append(CRLF)
            writer.append("${mappingFile.name}$CRLF")
            writer.flush()

            // Add file
            writer.append("--$boundary$CRLF")
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"${mappingFile.name}\"$CRLF")
            writer.append("Content-Type: application/octet-stream$CRLF")
            writer.append(CRLF)
            writer.flush()

            mappingFile.inputStream().use { it.copyTo(outputStream) }
            outputStream.flush()

            writer.append("$CRLF--$boundary--$CRLF")
            writer.close()
        }
    }

    private fun handleResponse(
        connection: HttpURLConnection,
        realm: String,
        applicationId: String,
        versionCode: Int,
        buildId: String,
        mappingFile: File
    ) {
        logger.debug("Upload", "Reading HTTP response")

        val responseCode = try {
            connection.responseCode
        } catch (e: IOException) {
            // HttpURLConnection failed to read the server's error response
            logger.error("Upload", "HTTP operation failed: ${e.message}")

            logger.error(
                "Upload",
                "Could not read server error response due to HttpURLConnection limitation when handling upload errors"
            )
            logger.error(
                "Upload",
                "For additional diagnostics, you can try the upload with curl (copy as single line):"
            )
            logger.error(
                "Upload",
                "curl -X PUT \"https://api.$realm.signalfx.com/v2/rum-mfm/proguard/$applicationId/$versionCode/$buildId\" -H \"X-SF-Token: YOUR_API_TOKEN_HERE\" -F \"filename=${mappingFile.name}\" -F \"file=@${mappingFile.absolutePath}\""
            )
            logger.error("Upload", "Replace YOUR_API_TOKEN_HERE with your actual API access token.")

            throw e
        }

        logger.info("Upload", "Response code: $responseCode")
        logger.info("Upload", "Response message: ${connection.responseMessage}")

        if (responseCode.isSuccessfulHttpCode()) {
            logger.debug("Upload", "Success response received")
            val response = connection.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            logger.lifecycle("Upload", "Upload successful (HTTP $responseCode)")
            if (response.isNotEmpty()) {
                logger.info("Upload", "Response body: $response")
            } else {
                logger.debug("Upload", "Empty response body")
            }
        } else {
            logger.warn("Upload", "Error response received (HTTP $responseCode)")
            val errorResponse =
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
            logger.error("Upload", "Error response body: $errorResponse")

            logger.debug("Upload", "Response headers:")
            connection.headerFields.forEach { (key, values) ->
                logger.debug("Upload", "Response header $key: ${values.joinToString(", ")}")
            }

            throw Exception("HTTP $responseCode")
        }
    }

    private fun Int.isSuccessfulHttpCode(): Boolean = this in 200..299
}
