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
import org.gradle.api.GradleException
import org.gradle.api.Project

class MappingFileUploader(private val project: Project) {

    private val logger = SplunkLogger(project.logger)
    private val httpClient = MappingFileUploadClient(logger)

    fun uploadAfterBuild(variant: ApplicationVariant, buildId: String, extension: SplunkRumExtension) {
        logger.info("Upload", "Starting upload process for variant '${variant.name}'")

        val accessToken = resolveAccessToken(extension)
        val realm = resolveRealm(extension)

        val missingConfigs = buildList {
            if (accessToken.isNullOrBlank()) add("accessToken")
            if (realm.isNullOrBlank()) add("realm")
        }

        if (missingConfigs.isNotEmpty()) {
            val errorMessage = "Missing required configuration: ${missingConfigs.joinToString(", ")}"
            val helpfulMessage = """
                Missing required configuration: ${missingConfigs.joinToString(", ")}
        
                Configure in your build.gradle:
                splunkRum {
                    ${if ("accessToken" in missingConfigs) "accessToken = \"your-token-here\"" else ""}
                    ${if ("realm" in missingConfigs) "realm = \"your-realm-here\"" else ""}
                }
        
                Or use environment variables: SPLUNK_ACCESS_TOKEN, SPLUNK_REALM
            """.trimIndent()

            if (extension.failBuildOnUploadFailure.get()) {
                logger.error("Config", helpfulMessage)
                throw GradleException("Mapping file upload failed for variant '${variant.name}': $errorMessage")
            } else {
                logger.warn("Config", "$helpfulMessage (build continuing due to failBuildOnUploadFailure=false)")
                return
            }
        }

        logger.debug(
            "Config",
            "Configuration resolved - realm: '$realm', token configured: ${!accessToken.isNullOrBlank()}"
        )

        val mappingFile = findMappingFile(variant)
        if (mappingFile == null) {
            val errorMessage = "Mapping file not found for variant '${variant.name}'"
            val detailedMessage = """
                Mapping file not found for variant '${variant.name}'
        
                Searched locations:
                ${getMappingFileLocations(variant).joinToString("\n") { "  - ${it.absolutePath}" }}
        
                This usually means:
                1. ProGuard/R8 minification is not properly configured
                2. The build did not complete successfully
                3. The mapping file is in a custom location
                        
                Please verify that minification is enabled and the build completed without errors.
                If the mapping file is in a custom location, please disable the plugin and upload the mapping file 
                using the Splunk O11Y Web UI or the Splunk RUM CLI tool.
            """.trimIndent()

            if (extension.failBuildOnUploadFailure.get()) {
                logger.error("File", detailedMessage)
                throw GradleException("Mapping file upload failed for variant '${variant.name}': $errorMessage")
            } else {
                logger.error("File", "$detailedMessage (build continuing due to failBuildOnUploadFailure=false)")
                return
            }
        }

        logger.info("File", "Found mapping file (${mappingFile.length()} bytes) at: ${mappingFile.absolutePath}")

        logger.lifecycle(
            "Upload",
            """
            Uploading mapping file for variant ${variant.name}
            Upload details:
                File: ${mappingFile.absolutePath}
                App ID: ${variant.applicationId}
                Version Code: ${variant.versionCode}
                Build ID: $buildId
            """.trimIndent()
        )
        logger.debug("Upload", "Realm: $realm")
        logger.debug("Upload", "File size: ${mappingFile.length()} bytes")

        try {
            httpClient.uploadMappingFile(
                mappingFile = mappingFile,
                buildId = buildId,
                applicationId = variant.applicationId,
                versionCode = variant.versionCode,
                accessToken = accessToken!!,
                realm = realm!!
            )
            logger.lifecycle("Upload", "Successfully uploaded mapping file")
        } catch (e: Exception) {
            logger.error("Upload", "Upload failed: ${e.message}")

            val errorMessage = "Upload failed: ${e.message}"

            if (extension.failBuildOnUploadFailure.get()) {
                logger.error("Upload", errorMessage)
                logger.debug("Upload", "Full error stacktrace: ${e.stackTraceToString()}")
                throw GradleException("Mapping file upload failed for variant '${variant.name}': ${e.message}")
            } else {
                logger.error("Upload", "$errorMessage (build continuing due to failBuildOnUploadFailure=false)")
                logger.debug("Upload", "Full error stacktrace: ${e.stackTraceToString()}")
            }
        }
    }

    private fun resolveAccessToken(extension: SplunkRumExtension): String? {
        logger.debug("Config", "Resolving access token")
        return extension.apiAccessToken.orNull
            ?: project.findProperty("splunk.accessToken") as String?
            ?: System.getenv("SPLUNK_ACCESS_TOKEN")
    }

    private fun resolveRealm(extension: SplunkRumExtension): String? {
        logger.debug("Config", "Resolving realm from multiple sources")
        return extension.realm.orNull
            ?: project.findProperty("splunk.realm") as String?
            ?: System.getenv("SPLUNK_REALM")
    }

    private fun findMappingFile(variant: ApplicationVariant): File? {
        val locations = getMappingFileLocations(variant)

        for ((index, location) in locations.withIndex()) {
            logger.debug("File", "Checking location ${index + 1}: ${location.absolutePath}")
            if (location.exists() && location.isFile) {
                if (index == 0) {
                    logger.debug("File", "Found mapping file at primary location")
                } else {
                    logger.info("File", "Found mapping file at fallback location: ${location.absolutePath}")
                }
                return location
            }
        }

        logger.debug("File", "No mapping file found in any expected location")
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
