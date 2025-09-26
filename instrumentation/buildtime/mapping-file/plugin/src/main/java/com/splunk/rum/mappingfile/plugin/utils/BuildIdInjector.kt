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
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Element

object BuildIdInjector {

    fun writeBuildIdToFile(buildDir: File, variantName: String, buildId: String) {
        val buildIdFile = File(buildDir, "tmp/splunk-build-id-$variantName.txt")
        buildIdFile.parentFile.mkdirs()
        buildIdFile.writeText(buildId)
    }

    fun readBuildIdFromFile(buildDir: File, variantName: String): String? {
        val buildIdFile = File(buildDir, "tmp/splunk-build-id-$variantName.txt")
        return if (buildIdFile.exists()) {
            buildIdFile.readText().trim()
        } else {
            null
        }
    }

    fun injectMetadataIntoMergedManifest(
        variantName: String,
        manifestOutputFiles: Set<File>,
        buildId: String,
        logger: SplunkLogger
    ) {
        try {
            logger.info("BuildId", "processManifest task outputs for variant '$variantName':")
            logger.info("BuildId", "Found ${manifestOutputFiles.size} output files:")
            manifestOutputFiles.forEach { file ->
                logger.info(
                    "BuildId",
                    "  - ${file.absolutePath} (exists: ${file.exists()}, isFile: ${file.isFile})"
                )

                // If it's a directory, look for AndroidManifest.xml inside it
                if (file.isDirectory) {
                    val manifestInDir = File(file, "AndroidManifest.xml")
                    logger.info(
                        "BuildId",
                        "    → Looking for AndroidManifest.xml: ${manifestInDir.absolutePath} (exists: ${manifestInDir.exists()})"
                    )
                }
            }

            // Updated logic to find the manifest - check both files and directories
            val manifestFile = manifestOutputFiles.firstNotNullOfOrNull { file ->
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
                logger.info("BuildId", "Found merged manifest: ${manifestFile.absolutePath}")
                val wasModified = addMetadataToManifest(manifestFile, buildId, logger)
                if (wasModified) {
                    logger.info("BuildId", "Successfully modified manifest")
                }
            } else {
                logger.warn("BuildId", "No AndroidManifest.xml found in task outputs")
            }
        } catch (e: Exception) {
            logger.warn("BuildId", "Could not access manifest via task outputs: ${e.message}")
        }
    }

    fun findManifestFiles(buildDir: File, variantName: String): Collection<File> {
        val manifestFiles = mutableListOf<File>()

        val possiblePaths = listOf(
            "intermediates/merged_manifests/$variantName/AndroidManifest.xml",
            "intermediates/merged_manifest/$variantName/AndroidManifest.xml",
            "intermediates/packaged_manifests/$variantName/AndroidManifest.xml",
            "outputs/apk/$variantName/AndroidManifest.xml"
        )

        possiblePaths.forEach { relativePath ->
            val manifestFile = File(buildDir, relativePath)
            if (manifestFile.exists()) {
                manifestFiles.add(manifestFile)
            }
        }

        return manifestFiles
    }

    fun injectMetadataIntoManifestFiles(
        variantName: String,
        manifestFiles: Collection<File>,
        buildId: String,
        logger: SplunkLogger
    ) {
        logger.info("BuildId", "Processing ${manifestFiles.size} manifest files for variant '$variantName'")

        manifestFiles.forEach { manifestFile ->
            if (manifestFile.exists() && manifestFile.name == "AndroidManifest.xml") {
                logger.info("BuildId", "Found manifest file: ${manifestFile.absolutePath}")
                val wasModified = addMetadataToManifest(manifestFile, buildId, logger)
                if (wasModified) {
                    logger.info("BuildId", "Successfully modified manifest")
                }
            }
        }
    }

    private fun addMetadataToManifest(manifestFile: File, buildId: String, logger: SplunkLogger): Boolean {
        logger.debug("BuildId", "Reading manifest file: ${manifestFile.absolutePath}")
        if (!manifestFile.canWrite()) {
            logger.warn("BuildId", "Manifest file is not writable: ${manifestFile.absolutePath}")
            return false
        }

        try {
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = documentBuilder.parse(manifestFile)
            logger.debug("BuildId", "Successfully parsed XML document")

            val applicationNodes = document.getElementsByTagName("application")
            if (applicationNodes.length == 0) {
                logger.error("BuildId", "Could not find <application> tag in: ${manifestFile.name}")
                return false
            }

            logger.debug("BuildId", "Found <application> tag in manifest")
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
                logger.info(
                    "BuildId",
                    "Removing ${toRemove.size} existing build ID metadata from: ${manifestFile.name}"
                )
                toRemove.forEach { applicationElement.removeChild(it) }
                logger.debug("BuildId", "Removed existing metadata entries")
            }

            // Add newly generated splunk.build_id metadata
            logger.debug("BuildId", "Adding metadata to <application> tag")
            val metaDataElement = document.createElement("meta-data")
            metaDataElement.setAttribute("android:name", "splunk.build_id")
            metaDataElement.setAttribute("android:value", buildId)
            applicationElement.appendChild(metaDataElement)

            // Write back to file without reformatting
            logger.debug("BuildId", "Writing modified XML back to file")
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "no")
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")

            val result = DOMSource(document)
            val streamResult = StreamResult(manifestFile)
            transformer.transform(result, streamResult)

            logger.lifecycle("BuildId", "Successfully injected build ID metadata into: ${manifestFile.name}")

            return true
        } catch (e: Exception) {
            logger.error("BuildId", "Error modifying ${manifestFile.name}: ${e.message}")
            logger.debug("BuildId", "Modification error details: ${e.stackTraceToString()}")
            return false
        }
    }
}