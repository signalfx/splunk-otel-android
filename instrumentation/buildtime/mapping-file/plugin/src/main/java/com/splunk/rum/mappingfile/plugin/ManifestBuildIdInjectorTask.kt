/*
 * Copyright 2026 Splunk Inc.
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

import com.splunk.rum.mappingfile.plugin.utils.BuildIdInjector
import com.splunk.rum.mappingfile.plugin.utils.SplunkLogger
import java.util.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ManifestBuildIdInjectorTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @get:Input
    abstract val variantName: Property<String>

    @get:Internal
    abstract val buildDirectory: DirectoryProperty

    @TaskAction
    fun taskAction() {
        val taskLogger = SplunkLogger(logger)
        val name = variantName.get()
        val buildDir = buildDirectory.get().asFile

        val buildId = UUID.randomUUID().toString()
        taskLogger.lifecycle("BuildId", "Generated build ID for variant '$name': $buildId")

        BuildIdInjector.writeBuildIdToFile(buildDir, name, buildId)

        val inputFile = mergedManifest.get().asFile
        val outputFile = updatedManifest.get().asFile
        inputFile.copyTo(outputFile, overwrite = true)

        val wasModified = BuildIdInjector.addMetadataToManifest(outputFile, buildId, taskLogger)
        if (wasModified) {
            taskLogger.info("BuildId", "Successfully injected build ID into manifest for variant '$name'")
        } else {
            taskLogger.warn("BuildId", "Failed to inject build ID into manifest for variant '$name'")
        }
    }
}
