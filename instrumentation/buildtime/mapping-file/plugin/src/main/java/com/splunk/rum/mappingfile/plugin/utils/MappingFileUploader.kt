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

import org.gradle.api.Project
import java.io.File

class MappingFileUploader(private val project: Project) {

    private val httpClient = SplunkHttpClient()

    fun uploadAfterBuild(variant: com.android.build.gradle.api.ApplicationVariant, buildId: String) {
        // TODO: create gradle plugin extension for customer to put in these values instead
        val accessToken = project.findProperty("splunk.accessToken") as String?
            ?: System.getenv("SPLUNK_ACCESS_TOKEN")
            ?: ""
        val realm = project.findProperty("splunk.realm") as String?
            ?: System.getenv("SPLUNK_REALM")
            ?: "lab0"

        // TODO: potentially recursively search for mapping file in case it isn't in this location
        val buildDir = project.layout.buildDirectory.get().asFile
        val mappingFile = File(buildDir, "outputs/mapping/${variant.name}/mapping.txt")

        if (!mappingFile.exists()) {
            println("Splunk RUM: Mapping file not found at ${mappingFile.absolutePath}, skipping upload")
            return
        }

        if (accessToken.isBlank() || realm.isBlank()) {
            val missingConfig = when {
                accessToken.isBlank() -> "Access token not configured"
                realm.isBlank() -> "Realm not configured"
                else -> "Unknown configuration error"
            }
            println("Splunk RUM: $missingConfig, skipping upload")
            return
        }

        println("Splunk RUM: Uploading mapping file for variant ${variant.name}")
        println("  File: ${mappingFile.absolutePath}")
        println("  App ID: ${variant.applicationId}")
        println("  Version Code: ${variant.versionCode}")
        println("  Build ID: $buildId")

        try {
            httpClient.uploadMappingFile(
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
}