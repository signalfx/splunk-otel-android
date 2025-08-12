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

import com.android.build.gradle.api.ApplicationVariant
import com.splunk.rum.mappingfile.plugin.SplunkRumExtension
import java.io.File
import org.gradle.api.Project

class MappingFileUploader(private val project: Project) {

    private val httpClient = MappingFileUploadClient(project.logger)

    fun uploadAfterBuild(variant: ApplicationVariant, buildId: String, extension: SplunkRumExtension) {
        project.logger.info("Splunk RUM: Starting upload process for variant '${variant.name}'")

        val accessToken = resolveAccessToken(extension)
        val realm = resolveRealm(extension)

        val missingConfigs = buildList {
            if (accessToken.isBlank()) add("accessToken")
            if (realm.isBlank()) add("realm")
        }

        if (missingConfigs.isNotEmpty()) {
            project.logger.warn("Splunk RUM: Missing required configuration: ${missingConfigs.joinToString(", ")}")
            project.logger.info("Splunk RUM: Configure in your build.gradle:")
            project.logger.info(
                """
        splunkRum {
            ${if ("accessToken" in missingConfigs) "accessToken = \"your-token-here\"" else ""}
            ${if ("realm" in missingConfigs) "realm = \"your-realm-here\"" else ""}
        }
                """.trimIndent()
            )
            project.logger.info("Splunk RUM: Or use environment variables: SPLUNK_ACCESS_TOKEN, SPLUNK_REALM")
            return
        }

        project.logger.debug(
            "Splunk RUM: Configuration resolved - realm: '$realm', token configured: ${accessToken.isNotBlank()}"
        )

        val mappingFile = findMappingFile(variant)
        if (mappingFile == null) {
            project.logger.error(
                """
                Splunk RUM: Mapping file not found for variant '${variant.name}'
                
                Searched locations:
                ${getMappingFileLocations(variant).joinToString("\n") { "  - ${it.absolutePath}" }}
                
                This usually means:
                1. ProGuard/R8 minification is not properly configured
                2. The build did not complete successfully
                3. The mapping file is in a custom location
                                
                Please verify that minification is enabled and the build completed without errors.
                If the mapping file is in a custom location, please disable the uploadEnabled and upload it manually.
                """.trimIndent()
            )
            return
        }

        project.logger.info(
            "Splunk RUM: Found mapping file (${mappingFile.length()} bytes) at: ${mappingFile.absolutePath}"
        )

        project.logger.lifecycle("Splunk RUM: Uploading mapping file for variant ${variant.name}")
        project.logger.info("Splunk RUM: Upload details:")
        project.logger.info("  File: ${mappingFile.absolutePath}")
        project.logger.info("  App ID: ${variant.applicationId}")
        project.logger.info("  Version Code: ${variant.versionCode}")
        project.logger.info("  Build ID: $buildId")
        project.logger.debug("  Realm: $realm")
        project.logger.debug("  File size: ${mappingFile.length()} bytes")

        try {
            httpClient.uploadMappingFile(
                mappingFile = mappingFile,
                buildId = buildId,
                applicationId = variant.applicationId,
                versionCode = variant.versionCode,
                accessToken = accessToken,
                realm = realm
            )
            project.logger.lifecycle("Splunk RUM: Successfully uploaded mapping file")
        } catch (e: Exception) {
            project.logger.error("Splunk RUM: Upload failed: ${e.message}")
            project.logger.debug("Splunk RUM: Full error stacktrace: ${e.stackTraceToString()}")
            // Don't fail the build, let app build finish
        }
    }

    private fun resolveAccessToken(extension: SplunkRumExtension): String {
        project.logger.debug("Splunk RUM: Resolving access token")
        return extension.apiAccessToken.orNull
            ?: project.findProperty("splunk.accessToken") as String?
            ?: System.getenv("SPLUNK_ACCESS_TOKEN")
            ?: ""
    }

    private fun resolveRealm(extension: SplunkRumExtension): String {
        project.logger.debug("Splunk RUM: Resolving realm from multiple sources")
        return extension.realm.orNull
            ?: project.findProperty("splunk.realm") as String?
            ?: System.getenv("SPLUNK_REALM")
            ?: ""
    }

    private fun findMappingFile(variant: ApplicationVariant): File? {
        val locations = getMappingFileLocations(variant)

        for ((index, location) in locations.withIndex()) {
            project.logger.debug("Splunk RUM: Checking location ${index + 1}: ${location.absolutePath}")
            if (location.exists() && location.isFile) {
                if (index == 0) {
                    project.logger.debug("Splunk RUM: Found mapping file at primary location")
                } else {
                    project.logger.info("Splunk RUM: Found mapping file at fallback location: ${location.absolutePath}")
                }
                return location
            }
        }

        project.logger.debug("Splunk RUM: No mapping file found in any expected location")
        return null
    }

    private fun getMappingFileLocations(variant: ApplicationVariant): List<File> {
        val buildDir = project.layout.buildDirectory.get().asFile
        val variantName = variant.name

        return listOf(
            // Standard mapping file location (AGP 7+ standard with R8)
            File(buildDir, "outputs/mapping/$variantName/mapping.txt"),

            // Fallback: build-type specific (some custom configurations)
            File(buildDir, "outputs/mapping/${variant.buildType.name}/mapping.txt")
        )
    }
}
