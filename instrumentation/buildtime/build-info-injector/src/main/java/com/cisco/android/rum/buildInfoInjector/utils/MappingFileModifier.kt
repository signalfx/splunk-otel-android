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

package com.cisco.android.rum.buildInfoInjector.utils

import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

class MappingFileModifier(private val pluginLogger: Logger) {
    companion object {
        const val OBFUSCATION_TYPE = "obfuscationType"
        const val HASH = "hash"
        const val MAPPING_FILE_NAME = "mapping.txt"
        const val CISCO_AGENT_BUILD_INFO_KEY = "ciscoAgentBuildInfo"
    }

    fun appendBuildInfoToMappingFile(
        project: Project,
        obfuscationType: String,
        hash: String?,
        variant: ApplicationVariant
    ) {
        val mappingFile = locateMappingFile(project, variant)
        if (mappingFile != null) {
            mappingFile.appendText(createBuildInfoJSONString(obfuscationType, hash))
        } else {
            pluginLogger.error("Attempted to locate mapping file to append app build and obfuscation info, " +
                    "but was unable to locate valid mapping.txt file")
        }
    }

    private fun locateMappingFile(project: Project, variant: ApplicationVariant): File? {
        val mappingFilePath = "${project.buildDir}/outputs/mapping/${variant.name}/${MAPPING_FILE_NAME}"
        val mappingFile = project.file(mappingFilePath)

        if (project.file(mappingFile).isFile) {
            return mappingFile
        } else {
            val projectDir = project.projectDir
            val filesFromRecursiveSearch = searchProjectForMappingFiles(projectDir)
            val mostRecentMappingFile = filesFromRecursiveSearch.maxByOrNull { it.lastModified() }
            if (mostRecentMappingFile != null) {
                pluginLogger.info("Could not locate mapping file in default location: ${mappingFilePath}" +
                        ", found and using mapping.txt file in: ${mostRecentMappingFile.path}")
            }
            return mostRecentMappingFile
        }
    }

    private fun searchProjectForMappingFiles(directory: File): List<File> {
        return directory.walk()
            .filter { it.isFile && it.name == MAPPING_FILE_NAME }
            .toList()
    }

    private fun createBuildInfoJSONString(obfuscationType: String, hash: String?): String {
        val buildInfoJsonStringBuilder = StringBuilder()

        buildInfoJsonStringBuilder.append("# {\"$CISCO_AGENT_BUILD_INFO_KEY\": {")
        buildInfoJsonStringBuilder.append("\"$OBFUSCATION_TYPE\":\"$obfuscationType\"")

        hash?.let { buildInfoJsonStringBuilder.append(",\"$HASH\":\"$it\"") }
        buildInfoJsonStringBuilder.append("}}\n")

        return buildInfoJsonStringBuilder.toString()
    }
}