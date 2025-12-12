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
import org.gradle.api.GradleException

class MappingFileUploader(private val buildDir: File, private val logger: SplunkLogger) {

    private val httpClient = MappingFileUploadClient(logger)

    fun uploadAfterBuild(
        variantName: String,
        applicationId: String,
        versionCode: Int,
        buildTypeName: String,
        buildId: String,
        accessToken: String?,
        realm: String?,
        failBuildOnUploadFailure: Boolean
    ) {
        logger.info("Upload", "Starting upload process for variant '$variantName'")

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

            if (failBuildOnUploadFailure) {
                logger.error("Config", helpfulMessage)
                throw GradleException("Mapping file upload failed for variant '$variantName': $errorMessage")
            } else {
                logger.warn("Config", "$helpfulMessage (build continuing due to failBuildOnUploadFailure=false)")
                return
            }
        }

        logger.debug(
            "Config",
            "Configuration resolved - realm: '$realm', token configured: ${!accessToken.isNullOrBlank()}"
        )

        val mappingFile = findMappingFile(variantName, buildTypeName)
        if (mappingFile == null) {
            val errorMessage = "Mapping file not found for variant '$variantName'"
            val detailedMessage = """
                Mapping file not found for variant '$variantName'
        
                Searched locations:
                ${getMappingFileLocations(variantName, buildTypeName).joinToString("\n") { "  - ${it.absolutePath}" }}
        
                This usually means:
                1. ProGuard/R8 minification is not properly configured
                2. The build did not complete successfully
                3. The mapping file is in a custom location
                        
                Please verify that minification is enabled and the build completed without errors.
                If the mapping file is in a custom location, please disable the plugin and upload the mapping file 
                using the Splunk O11Y Web UI or the Splunk RUM CLI tool.
            """.trimIndent()

            if (failBuildOnUploadFailure) {
                logger.error("File", detailedMessage)
                throw GradleException("Mapping file upload failed for variant '$variantName': $errorMessage")
            } else {
                logger.error("File", "$detailedMessage (build continuing due to failBuildOnUploadFailure=false)")
                return
            }
        }

        logger.info("File", "Found mapping file (${mappingFile.length()} bytes) at: ${mappingFile.absolutePath}")

        logger.lifecycle(
            "Upload",
            """
            Uploading mapping file for variant $variantName
            Upload details:
                File: ${mappingFile.absolutePath}
                App ID: $applicationId
                Version Code: $versionCode
                Build ID: $buildId
            """.trimIndent()
        )
        logger.debug("Upload", "Realm: $realm")
        logger.debug("Upload", "File size: ${mappingFile.length()} bytes")

        try {
            httpClient.uploadMappingFile(
                mappingFile = mappingFile,
                buildId = buildId,
                applicationId = applicationId,
                versionCode = versionCode,
                accessToken = accessToken!!,
                realm = realm!!
            )
            logger.lifecycle("Upload", "Successfully uploaded mapping file")
        } catch (e: Exception) {
            logger.error("Upload", "Full error stacktrace: ${e.stackTraceToString()}")

            if (failBuildOnUploadFailure) {
                logger.error("Upload", "Upload failed: ${e.message}")
                throw GradleException(
                    "Mapping file upload failed for variant '$variantName': ${e.message}. " +
                        "Check the full build log above for error details and troubleshooting steps."
                )
            } else {
                logger.error(
                    "Upload",
                    "Upload failed: ${e.message} (build continuing due to failBuildOnUploadFailure=false)"
                )
            }
        }
    }

    private fun findMappingFile(variantName: String, buildTypeName: String): File? {
        val locations = getMappingFileLocations(variantName, buildTypeName)

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

    private fun getMappingFileLocations(variantName: String, buildTypeName: String): List<File> = listOf(
        File(buildDir, "outputs/mapping/$variantName/mapping.txt"),
        File(buildDir, "outputs/mapping/$buildTypeName/mapping.txt")
    )
}
