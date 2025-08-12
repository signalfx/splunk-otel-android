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

import com.splunk.rum.mappingfile.plugin.SplunkRumExtension
import com.android.build.gradle.api.ApplicationVariant
import java.io.File
import java.util.*
import org.gradle.api.Project

class BuildIdInjector(private val project: Project) {

    fun injectBuildId(variant: ApplicationVariant,
                      buildId: String,
                      extension: SplunkRumExtension
    ) {
        project.logger.info("Splunk RUM: Setting up build ID injection for variant '${variant.name}'")

        variant.outputs.forEach { output ->
            try {
                val processManifestTask = output.processManifest
                project.logger.debug("Splunk RUM: Hooking into processManifest task: ${processManifestTask.name}")

                processManifestTask.doLast {
                    project.logger.info("Splunk RUM: Executing injection after processManifest")
                    injectMetadataIntoMergedManifest(variant, buildId, extension)
                }
                project.logger.info("Splunk RUM: Hooked into ${processManifestTask.name}")
            } catch (e: Exception) {
                project.logger.warn("Splunk RUM: Could not hook processManifest: ${e.message}")
                project.logger.debug("Splunk RUM: Falling back to package task hook")
                val packageTaskName = "package${variant.name.capitalize(Locale.ROOT)}"
                project.tasks.named(packageTaskName).configure { task ->
                    task.doFirst {
                        project.logger.info("Splunk RUM: Executing injection via package task fallback")
                        injectMetadataIntoMergedManifest(variant, buildId, extension)
                    }
                }
                project.logger.info("Splunk RUM: Used fallback hook into $packageTaskName")
            }
        }
    }

    private fun injectMetadataIntoMergedManifest(
        variant: ApplicationVariant,
        buildId: String,
        extension: SplunkRumExtension
    ) {
        project.logger.info("Splunk RUM: Searching for manifest files for variant '${variant.name}'")

        val buildDir = project.layout.buildDirectory.get().asFile
        project.logger.debug("Splunk RUM: Build directory: ${buildDir.absolutePath}")

        val manifestFiles = buildDir.walkTopDown()
            .filter { it.name == "AndroidManifest.xml" && it.absolutePath.contains(variant.name) }
            .filter { it.exists() }
            .toList()

        if (manifestFiles.isNotEmpty()) {
            project.logger.info("Splunk RUM: Found ${manifestFiles.size} manifest files:")
            manifestFiles.forEach { file ->
                project.logger.info("  - ${file.absolutePath}")
            }

            var successCount = 0
            manifestFiles.forEach { manifestFile ->
                project.logger.debug("Splunk RUM: Processing manifest file: ${manifestFile.name}")
                try {
                    val wasModified = addMetadataToManifest(manifestFile, buildId, extension)
                    if (wasModified) {
                        successCount++
                        project.logger.info("Splunk RUM: Modified: ${manifestFile.name}")
                    }
                } catch (e: Exception) {
                    project.logger.error("Splunk RUM: Failed: ${manifestFile.name} - ${e.message}")
                    project.logger.debug("Splunk RUM: Error details: ${e.stackTraceToString()}")
                }
            }
            project.logger.lifecycle("Splunk RUM: Successfully modified $successCount/${manifestFiles.size} files")
        } else {
            project.logger.warn("Splunk RUM: No manifest files found for variant ${variant.name}")
        }
    }

    private fun addMetadataToManifest(manifestFile: File,
                                      buildId: String,
                                      extension: SplunkRumExtension): Boolean {
        project.logger.debug("Splunk RUM: Reading manifest file: ${manifestFile.absolutePath}")

        try {
            var content = manifestFile.readText()

            val existingMetadataPattern = Regex(
                "<meta-data\\s+android:name=\"splunk\\.build_id\"[^>]*(?:/>|>[^<]*</meta-data>)",
                RegexOption.DOT_MATCHES_ALL
            )
            if (existingMetadataPattern.containsMatchIn(content)) {
                project.logger.info("Splunk RUM: Removing existing build ID metadata from: ${manifestFile.name}")
                content = content.replace(existingMetadataPattern, "")
                project.logger.debug("Splunk RUM: Removed existing metadata, new size: ${content.length} characters")
            }

            // Enhanced regex that handles both opening tags and self-closing tags
            val applicationPattern = Regex("<application([^>]*?)(?:/>|>)", RegexOption.DOT_MATCHES_ALL)
            val match = applicationPattern.find(content)

            if (match != null) {
                project.logger.debug("Splunk RUM: Found <application> tag in manifest")
                val fullMatch = match.value
                val metadataTag = "\n        <meta-data android:name=\"splunk.build_id\" android:value=\"$buildId\" />"

                val replacement = if (fullMatch.endsWith("/>")) {
                    project.logger.debug("Splunk RUM: Converting self-closing <application/> tag")
                    val attributes = match.groupValues[1]
                    "<application$attributes>$metadataTag\n    </application>"
                } else {
                    project.logger.debug("Splunk RUM: Adding metadata to existing <application> tag")
                    "$fullMatch$metadataTag"
                }

                content = content.replace(fullMatch, replacement)
                manifestFile.writeText(content)
                project.logger.lifecycle("Splunk RUM: Successfully injected build ID metadata into: ${manifestFile.name}")
                return true
            } else {
                project.logger.error("Splunk RUM: Could not find <application> tag in: ${manifestFile.name}")
                return false
            }
        } catch (e: Exception) {
            project.logger.error("Splunk RUM: Error modifying ${manifestFile.name}: ${e.message}")
            project.logger.debug("Splunk RUM: Modification error details: ${e.stackTraceToString()}")
            return false
        }
    }
}
