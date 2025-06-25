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

package com.splunk.rum.mappingfile.plugin

import com.android.build.gradle.AppExtension
import java.io.File
import java.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project

class MappingFilePlugin : Plugin<Project> {

    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project
        // TODO: replace all println with more proper logging
        println("Splunk RUM: Mapping File Plugin applied!")

        project.afterEvaluate {
            setupBuildIdInjection()
        }
    }

    private fun setupBuildIdInjection() {
        val android = project.extensions.findByType(AppExtension::class.java)
        if (android == null) {
            println("Splunk RUM: Android application plugin not found, skipping setup")
            return
        }

        android.applicationVariants.configureEach { variant ->
            if (variant.buildType.isMinifyEnabled) {
                injectBuildIdForVariant(variant)
            } else {
                println("Splunk RUM: Skipping variant '${variant.name}' as minification not enabled")
            }
        }
    }

    private fun injectBuildIdForVariant(variant: com.android.build.gradle.api.ApplicationVariant) {
        // TODO: check that customer does not have existing manual setup, if so, skip this process and log notice

        val buildId = UUID.randomUUID().toString()
        println("Splunk RUM: Generated build ID for variant '${variant.name}': $buildId")

        injectViaManifestModification(variant, buildId)
        setupUploadTask(variant, buildId)
    }

    private fun setupUploadTask(variant: com.android.build.gradle.api.ApplicationVariant, buildId: String) {
        // Hook into the assemble task to upload after build completes
        val assembleTaskName = "assemble${variant.name.capitalize(Locale.ROOT)}"
        project.tasks.named(assembleTaskName).configure { assembleTask ->
            assembleTask.doLast {
                uploadMappingFileAfterBuild(variant, buildId)
            }
        }
    }

    private fun uploadMappingFileAfterBuild(variant: com.android.build.gradle.api.ApplicationVariant, buildId: String) {
        // TODO: create gradle plugin extension for customer to put in these values instead
        val accessToken = project.findProperty("splunk.accessToken") as String?
            ?: System.getenv("SPLUNK_ACCESS_TOKEN")
            ?: ""
        val realm = project.findProperty("splunk.realm") as String?
            ?: System.getenv("SPLUNK_REALM")
            ?: "lab0"

        // TODO: potentially recursively search for mapping file in case it isnt in this location
        val buildDir = project.layout.buildDirectory.get().asFile
        val mappingFile = File(buildDir, "outputs/mapping/${variant.name}/mapping.txt")

        if (!mappingFile.exists()) {
            println("Splunk RUM: Mapping file not found at ${mappingFile.absolutePath}, skipping upload")
            return
        }

        if (accessToken.isBlank() || realm.isBlank()) {
            println("Splunk RUM: Access token or realm not configured, skipping upload")
            return
        }

        println("Splunk RUM: Uploading mapping file for variant ${variant.name}")
        println("  File: ${mappingFile.absolutePath}")
        println("  App ID: ${variant.applicationId}")
        println("  Version Code: ${variant.versionCode}")
        println("  Build ID: $buildId")

        try {
            uploadMappingFile(
                mappingFile = mappingFile,
                buildId = buildId,
                applicationId = variant.applicationId,
                versionCode = variant.versionCode,
                accessToken = accessToken,
                realm = realm
            )
            println("Splunk RUM: Successfully uploaded mapping file")
        } catch (e: Exception) {
            println("Splunk RUM: Upload failed: ${e.message}")
            // Don't fail the build, let app build finish
        }
    }

    private fun injectViaManifestModification(
        variant: com.android.build.gradle.api.ApplicationVariant,
        buildId: String
    ) {
        variant.outputs.forEach { output ->
            try {
                val processManifestTask = output.processManifest
                processManifestTask.doLast {
                    println("Splunk RUM: Executing injection after processManifest")
                    injectMetadataIntoMergedManifest(variant, buildId)
                }
                println("Splunk RUM: Hooked into ${processManifestTask.name}")
            } catch (e: Exception) {
                println("Splunk RUM: Could not hook processManifest: ${e.message}")
                val packageTaskName = "package${variant.name.capitalize(Locale.ROOT)}"
                project.tasks.named(packageTaskName).configure { task ->
                    task.doFirst {
                        injectMetadataIntoMergedManifest(variant, buildId)
                    }
                }
            }
        }
    }

    private fun injectMetadataIntoMergedManifest(
        variant: com.android.build.gradle.api.ApplicationVariant,
        buildId: String
    ) {
        println("Splunk RUM: Searching for manifest files")

        val buildDir = project.layout.buildDirectory.get().asFile
        val manifestFiles = buildDir.walkTopDown()
            .filter { it.name == "AndroidManifest.xml" && it.absolutePath.contains(variant.name) }
            .filter { it.exists() }
            .toList()

        if (manifestFiles.isNotEmpty()) {
            println("Splunk RUM: Found ${manifestFiles.size} manifest files:")
            manifestFiles.forEach { file ->
                println("  - ${file.absolutePath}")
            }

            var successCount = 0
            manifestFiles.forEach { manifestFile ->
                try {
                    val wasModified = addMetadataToManifest(manifestFile, buildId)
                    if (wasModified) {
                        successCount++
                        println("Splunk RUM: Modified: ${manifestFile.name}")
                    }
                } catch (e: Exception) {
                    println("Splunk RUM: Failed: ${manifestFile.name} - ${e.message}")
                }
            }

            println("Splunk RUM: Successfully modified $successCount/${manifestFiles.size} files")
        } else {
            println("Splunk RUM: No manifest files found for variant ${variant.name}")
        }
    }

    private fun addMetadataToManifest(manifestFile: java.io.File, buildId: String): Boolean {
        try {
            var content = manifestFile.readText()

            // Find <application> tag and inject metadata after it
            val applicationPattern = Regex("<application([^>]*)>")
            val match = applicationPattern.find(content)

            if (match != null) {
                val metadataTag = "\n        <meta-data android:name=\"splunk.build_id\" android:value=\"$buildId\" />"
                val replacement = "${match.value}$metadataTag"
                content = content.replace(match.value, replacement)

                manifestFile.writeText(content)
                println("Splunk RUM: Successfully injected build ID metadata into: ${manifestFile.name}")
                return true
            } else {
                println("Splunk RUM: Could not find <application> tag in: ${manifestFile.name}")
                return false
            }
        } catch (e: Exception) {
            println("Splunk RUM: Error modifying ${manifestFile.name}: ${e.message}")
            return false
        }
    }

    private fun uploadMappingFile(
        mappingFile: File,
        buildId: String,
        applicationId: String,
        versionCode: Int,
        accessToken: String,
        realm: String
    ) {
        // Build URL
        val url = "https://api.$realm.signalfx.com/v2/rum-mfm/proguard/$applicationId/$versionCode/$buildId"
        println("Splunk RUM: Upload URL: $url")
        println("Splunk RUM: Using PUT method")
        println("Splunk RUM: File size: ${mappingFile.length()} bytes")

        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection

        try {
            // Setup connection, PUT request
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.doInput = true
            connection.setRequestProperty("X-SF-Token", accessToken)

            // Create multipart formdata
            val boundary = "----Boundary${System.currentTimeMillis()}"
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

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

            // Response handling
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

                // Printing response headers for debugging
                connection.headerFields.forEach { (key, values) ->
                    println("Splunk RUM: Response header $key: ${values.joinToString(", ")}")
                }

                throw Exception("HTTP $responseCode: $errorResponse")
            }
        } finally {
            connection.disconnect()
        }
    }
}
