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
import java.util.*

class BuildIdInjector(private val project: Project) {

    fun injectBuildId(variant: com.android.build.gradle.api.ApplicationVariant, buildId: String) {
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

    private fun addMetadataToManifest(manifestFile: File, buildId: String): Boolean {
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
}