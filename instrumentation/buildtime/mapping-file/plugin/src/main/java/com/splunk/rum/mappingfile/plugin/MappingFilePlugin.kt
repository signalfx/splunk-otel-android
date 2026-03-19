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

package com.splunk.rum.mappingfile.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.gradle.AppPlugin
import com.splunk.rum.mappingfile.plugin.utils.BuildIdInjector
import com.splunk.rum.mappingfile.plugin.utils.MappingFileUploader
import com.splunk.rum.mappingfile.plugin.utils.SplunkLogger
import java.io.File
import java.util.*
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class MappingFilePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var logger: SplunkLogger
    private lateinit var extension: SplunkRumExtension

    override fun apply(project: Project) {
        this.project = project
        this.logger = SplunkLogger(project.logger)

        logger.lifecycle("Setup", "Mapping File Plugin applied")

        extension = project.extensions.create("splunkRum", SplunkRumExtension::class.java)
        logger.debug("Setup", "Created splunkRum extension")

        project.plugins.withType(AppPlugin::class.java) {
            logger.debug("Setup", "Found Android application plugin")

            val androidComponents = project.extensions.getByType(
                ApplicationAndroidComponentsExtension::class.java
            )

            androidComponents.onVariants { variant ->
                if (!extension.enabled.get()) {
                    logger.lifecycle("Setup", "Plugin is disabled via configuration")
                    return@onVariants
                }

                if (variant.isMinifyEnabled) {
                    logger.info("Setup", "Processing variant '${variant.name}' (minification enabled)")
                    processVariant(variant)
                } else {
                    logger.info("Setup", "Skipping variant '${variant.name}' as minification not enabled")
                }
            }
        }
    }

    private fun processVariant(variant: ApplicationVariant) {
        val variantName = variant.name
        val buildTypeName = variant.buildType ?: "unknown"
        val buildDir = project.layout.buildDirectory.get().asFile

        logger.debug("Setup", "Starting processing for variant '$variantName'")
        logger.info("Setup", "Setting up build ID injection and upload for variant '$variantName'")

        setupBuildIdInjectionForVariant(variant, variantName)

        setupUploadTask(variant, variantName, buildTypeName, buildDir)
    }

    private fun setupBuildIdInjectionForVariant(variant: ApplicationVariant, variantName: String) {
        logger.debug("BuildId", "Registering manifest transform task for variant '$variantName'")

        val manifestInjector = project.tasks.register(
            "splunkInjectBuildId${variantName.replaceFirstChar { it.uppercase() }}",
            ManifestBuildIdInjectorTask::class.java
        ) {
            it.variantName.set(variantName)
            it.buildDirectory.set(project.layout.buildDirectory)
            it.outputs.upToDateWhen { false }
        }

        variant.artifacts.use(manifestInjector)
            .wiredWithFiles(
                ManifestBuildIdInjectorTask::mergedManifest,
                ManifestBuildIdInjectorTask::updatedManifest
            )
            .toTransform(SingleArtifact.MERGED_MANIFEST)

        logger.info("BuildId", "Registered manifest transform for variant '$variantName'")
    }

    private fun setupUploadTask(
        variant: ApplicationVariant,
        variantName: String,
        buildTypeName: String,
        buildDir: File
    ) {
        val assembleTaskName = "assemble${variantName.replaceFirstChar { it.uppercase() }}"
        logger.debug("Upload", "Hooking into task '$assembleTaskName' for upload")

        project.afterEvaluate {
            val assembleTask = project.tasks.named(assembleTaskName)
            assembleTask.configure { taskConfig ->
                taskConfig.doLast { executingTask ->
                    val applicationId = variant.applicationId.get()
                    val versionCode = variant.outputs.firstOrNull()?.versionCode?.orNull ?: 0
                    val accessToken = extension.apiAccessToken.orNull
                        ?: System.getenv("SPLUNK_ACCESS_TOKEN")
                    val realm = extension.realm.orNull ?: System.getenv("SPLUNK_REALM")
                    val failBuildOnUploadFailure = extension.failBuildOnUploadFailure.get()

                    TaskActions.executeUploadTask(
                        task = executingTask,
                        buildDir = buildDir,
                        variantName = variantName,
                        applicationId = applicationId,
                        versionCode = versionCode,
                        buildTypeName = buildTypeName,
                        accessToken = accessToken,
                        realm = realm,
                        failBuildOnUploadFailure = failBuildOnUploadFailure
                    )
                }
            }
            logger.info("Upload", "Successfully configured upload task for variant '$variantName'")
        }
    }
}

internal object TaskActions {

    fun executeUploadTask(
        task: Task,
        buildDir: File,
        variantName: String,
        applicationId: String,
        versionCode: Int,
        buildTypeName: String,
        accessToken: String?,
        realm: String?,
        failBuildOnUploadFailure: Boolean
    ) {
        val taskLogger = SplunkLogger(task.logger)

        taskLogger.debug("Upload", "Assemble task completed, starting upload process")

        val buildId = BuildIdInjector.readBuildIdFromFile(buildDir, variantName)
            ?: run {
                val errorMessage = "Build ID file not found for variant '$variantName'. " +
                    "This indicates the manifest injection task didn't complete successfully."
                taskLogger.error("Upload", errorMessage)
                throw GradleException("Mapping file upload failed for variant '$variantName': $errorMessage")
            }

        taskLogger.lifecycle("Upload", "Using build ID for variant '$variantName': $buildId")

        val uploader = MappingFileUploader(buildDir, taskLogger)
        uploader.uploadAfterBuild(
            variantName = variantName,
            applicationId = applicationId,
            versionCode = versionCode,
            buildTypeName = buildTypeName,
            buildId = buildId,
            accessToken = accessToken,
            realm = realm,
            failBuildOnUploadFailure = failBuildOnUploadFailure
        )
    }
}
