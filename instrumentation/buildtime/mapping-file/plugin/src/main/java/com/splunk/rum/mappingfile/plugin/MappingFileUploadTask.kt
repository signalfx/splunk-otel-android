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
import com.splunk.rum.mappingfile.plugin.utils.MappingFileUploader
import com.splunk.rum.mappingfile.plugin.utils.SplunkLogger
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class MappingFileUploadTask : DefaultTask() {

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    abstract val applicationId: Property<String>

    @get:Input
    @get:Optional
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val buildTypeName: Property<String>

    @get:Input
    @get:Optional
    abstract val accessToken: Property<String>

    @get:Input
    @get:Optional
    abstract val realm: Property<String>

    @get:Input
    abstract val failBuildOnUploadFailure: Property<Boolean>

    @get:Internal
    abstract val buildDirectory: DirectoryProperty

    @TaskAction
    fun taskAction() {
        val taskLogger = SplunkLogger(logger)
        val name = variantName.get()
        val fail = failBuildOnUploadFailure.get()

        val vc = versionCode.orNull
        if (vc == null) {
            val msg = "versionCode is not set for variant '$name'. " +
                "Cannot upload mapping file without a valid versionCode."
            if (fail) {
                throw GradleException("Mapping file upload failed for variant '$name': $msg")
            } else {
                taskLogger.warn("Upload", "$msg Skipping upload.")
                return
            }
        }

        val buildDir = buildDirectory.get().asFile

        val buildId = BuildIdInjector.readBuildIdFromFile(buildDir, name)
            ?: run {
                val errorMessage = "Build ID file not found for variant '$name'. " +
                    "This indicates the manifest injection task didn't complete successfully."
                taskLogger.error("Upload", errorMessage)
                throw GradleException("Mapping file upload failed for variant '$name': $errorMessage")
            }

        taskLogger.lifecycle("Upload", "Using build ID for variant '$name': $buildId")

        val uploader = MappingFileUploader(buildDir, taskLogger)
        uploader.uploadAfterBuild(
            variantName = name,
            applicationId = applicationId.get(),
            versionCode = vc,
            buildTypeName = buildTypeName.get(),
            buildId = buildId,
            accessToken = accessToken.orNull,
            realm = realm.orNull,
            failBuildOnUploadFailure = fail
        )
    }
}
