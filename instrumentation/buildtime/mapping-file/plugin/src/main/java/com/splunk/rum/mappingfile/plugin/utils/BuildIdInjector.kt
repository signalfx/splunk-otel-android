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
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.gradle.api.Project
import org.w3c.dom.Element

class BuildIdInjector(private val project: Project) {

    fun injectBuildId(variant: ApplicationVariant, buildId: String) {
        project.logger.info("Splunk RUM: Setting up build ID injection for variant '${variant.name}'")

        variant.outputs.forEach { output ->
            try {
                val processManifestTaskProvider = output.processManifestProvider
                val processManifestTask = processManifestTaskProvider.get()
                project.logger.debug("Splunk RUM: Hooking into processManifest task: ${processManifestTask.name}")

                processManifestTask.doLast {
                    project.logger.info("Splunk RUM: Executing injection after processManifest")
                    injectMetadataIntoMergedManifest(variant, buildId)
                }
                project.logger.info("Splunk RUM: Hooked into ${processManifestTask.name}")
            } catch (e: Exception) {
                project.logger.warn("Splunk RUM: Could not hook processManifest: ${e.message}")
                project.logger.debug("Splunk RUM: Falling back to package task hook")
                val packageTaskName = "package${variant.name.capitalize(Locale.ROOT)}"
                project.tasks.named(packageTaskName).configure { task ->
                    task.doFirst {
                        project.logger.info("Splunk RUM: Executing injection via package task fallback")
                        injectMetadataIntoMergedManifest(variant, buildId)
                    }
                }
                project.logger.info("Splunk RUM: Used fallback hook into $packageTaskName")
            }
        }
    }
    private fun injectMetadataIntoMergedManifest(variant: ApplicationVariant, buildId: String) {
        variant.outputs.forEach { output ->
            try {
                val processManifestTaskProvider = output.processManifestProvider
                val processManifestTask = processManifestTaskProvider.get()
                val outputFiles = processManifestTask.outputs.files.files

                println("Splunk RUM: processManifest task outputs for variant '${variant.name}':")
                println("Splunk RUM: Found ${outputFiles.size} output files:")
                outputFiles.forEach { file ->
                    println("  - ${file.absolutePath} (exists: ${file.exists()}, isFile: ${file.isFile})")

                    // If it's a directory, look for AndroidManifest.xml inside it
                    if (file.isDirectory) {
                        val manifestInDir = File(file, "AndroidManifest.xml")
                        println(
                            "    → Looking for AndroidManifest.xml: ${manifestInDir.absolutePath} (exists: ${manifestInDir.exists()})"
                        )
                    }
                }

                // Updated logic to find the manifest - check both files and directories
                val manifestFile = outputFiles.firstNotNullOfOrNull { file ->
                    when {
                        file.isFile && file.name == "AndroidManifest.xml" && file.exists() -> file
                        file.isDirectory -> {
                            val manifestInDir = File(file, "AndroidManifest.xml")
                            if (manifestInDir.exists()) manifestInDir else null
                        }
                        else -> null
                    }
                }

                if (manifestFile != null) {
                    project.logger.info("Splunk RUM: Found merged manifest: ${manifestFile.absolutePath}")
                    val wasModified = addMetadataToManifest(manifestFile, buildId)
                    if (wasModified) {
                        project.logger.info("Splunk RUM: Successfully modified manifest")
                    }
                } else {
                    project.logger.warn("Splunk RUM: No AndroidManifest.xml found in task outputs")
                }
            } catch (e: Exception) {
                project.logger.warn("Splunk RUM: Could not access manifest via task outputs: ${e.message}")
            }
        }
    }

    private fun addMetadataToManifest(manifestFile: File, buildId: String): Boolean {
        project.logger.debug("Splunk RUM: Reading manifest file: ${manifestFile.absolutePath}")
        if (!manifestFile.canWrite()) {
            project.logger.warn("Splunk RUM: Manifest file is not writable: ${manifestFile.absolutePath}")
            return false
        }

        try {
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = documentBuilder.parse(manifestFile)
            project.logger.debug("Splunk RUM: Successfully parsed XML document")

            val applicationNodes = document.getElementsByTagName("application")
            if (applicationNodes.length == 0) {
                project.logger.error("Splunk RUM: Could not find <application> tag in: ${manifestFile.name}")
                return false
            }

            project.logger.debug("Splunk RUM: Found <application> tag in manifest")
            val applicationElement = applicationNodes.item(0) as Element

            // Collect potentially existing splunk.build_id to remove (from manual setup or previous runs)
            val toRemove = mutableListOf<Element>()
            val existingMetadata = applicationElement.getElementsByTagName("meta-data")
            for (i in 0 until existingMetadata.length) {
                val metaData = existingMetadata.item(i) as Element
                if (metaData.getAttribute("android:name") == "splunk.build_id") {
                    toRemove.add(metaData)
                }
            }

            // Remove all existing splunk.build_id metadata
            if (toRemove.isNotEmpty()) {
                project.logger.info(
                    "Splunk RUM: Removing ${toRemove.size} existing build ID metadata from: ${manifestFile.name}"
                )
                toRemove.forEach { applicationElement.removeChild(it) }
                project.logger.debug("Splunk RUM: Removed existing metadata entries")
            }

            // Add newly generated splunk.build_id metadata
            project.logger.debug("Splunk RUM: Adding metadata to <application> tag")
            val metaDataElement = document.createElement("meta-data")
            metaDataElement.setAttribute("android:name", "splunk.build_id")
            metaDataElement.setAttribute("android:value", buildId)
            applicationElement.appendChild(metaDataElement)

            // Write back to file without reformatting
            project.logger.debug("Splunk RUM: Writing modified XML back to file")
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "no")
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")

            val result = DOMSource(document)
            val streamResult = StreamResult(manifestFile)
            transformer.transform(result, streamResult)

            project.logger.lifecycle(
                "Splunk RUM: Successfully injected build ID metadata into: ${manifestFile.name}"
            )

            return true
        } catch (e: Exception) {
            project.logger.error("Splunk RUM: Error modifying ${manifestFile.name}: ${e.message}")
            project.logger.debug("Splunk RUM: Modification error details: ${e.stackTraceToString()}")
            return false
        }
    }
}
