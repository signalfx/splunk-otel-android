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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
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

        project.afterEvaluate {
            logger.debug("Setup", "Evaluating plugin configuration")
            if (extension.enabled.get()) {
                logger.info("Setup", "Plugin is enabled, proceeding with setup")
                setupBuildIdInjection()
            } else {
                logger.lifecycle("Setup", "Plugin is disabled via configuration")
            }
        }
    }

    private fun setupBuildIdInjection() {
        logger.debug("Setup", "Looking for Android application plugin")
        val android = project.extensions.findByType(AppExtension::class.java)
        if (android == null) {
            logger.warn("Setup", "Android application plugin not found, skipping setup")
            return
        }

        logger.info("Setup", "Found Android application plugin, configuring variants")

        // Extract buildDir at configuration time
        val buildDir = project.layout.buildDirectory.get().asFile

        android.applicationVariants.configureEach { variant ->
            logger.debug("Setup", "Evaluating variant '${variant.name}'")
            if (variant.buildType.isMinifyEnabled) {
                logger.info("Setup", "Processing variant '${variant.name}' (minification enabled)")
                processVariant(variant, buildDir)
            } else {
                logger.info("Setup", "Skipping variant '${variant.name}' as minification not enabled")
            }
        }
    }

    private fun processVariant(variant: ApplicationVariant, buildDir: File) {
        logger.debug("Setup", "Starting processing for variant '${variant.name}'")

        // Extract all needed data from variant and extension at configuration time
        val variantName = variant.name
        val applicationId = variant.applicationId
        val versionCode = variant.versionCode
        val buildTypeName = variant.buildType.name
        val accessToken = extension.apiAccessToken.orNull ?: System.getenv("SPLUNK_ACCESS_TOKEN")
        val realm = extension.realm.orNull ?: System.getenv("SPLUNK_REALM")
        val failBuildOnUploadFailure = extension.failBuildOnUploadFailure.get()

        // Setting up injection of build ID into manifest
        logger.info("Setup", "Setting up build ID injection and upload for variant '$variantName'")
        logger.debug("BuildId", "Initiating build ID injection for variant '$variantName'")

        setupBuildIdInjectionForVariant(variant, variantName, buildDir)

        // Set up upload task
        logger.info("Upload", "Setting up upload task for variant '$variantName'")
        setupUploadTask(
            variantName = variantName,
            applicationId = applicationId,
            versionCode = versionCode,
            buildTypeName = buildTypeName,
            buildDir = buildDir,
            accessToken = accessToken,
            realm = realm,
            failBuildOnUploadFailure = failBuildOnUploadFailure
        )
    }

    private fun setupBuildIdInjectionForVariant(variant: ApplicationVariant, variantName: String, buildDir: File) {
        variant.outputs.forEach { output ->
            try {
                val processManifestTaskProvider = output.processManifestProvider
                val processManifestTask = processManifestTaskProvider.get()

                // Extract the actual File paths at configuration time, not the FileCollection
                val manifestOutputFiles = processManifestTask.outputs.files.files

                processManifestTask.outputs.upToDateWhen { false }
                processManifestTask.doLast { task ->
                    // Call static method to avoid capturing plugin instance
                    TaskActions.executeBuildIdInjection(
                        task = task,
                        buildDir = buildDir,
                        variantName = variantName,
                        manifestOutputFiles = manifestOutputFiles
                    )
                }
            } catch (e: Exception) {
                // Fallback approach
                logger.warn("BuildId", "Could not hook processManifest: ${e.message}")
                logger.debug("BuildId", "Falling back to package task hook")

                val packageTaskName = "package${variant.name.capitalize(Locale.ROOT)}"

                val packageTask = project.tasks.named(packageTaskName)
                packageTask.configure { taskConfig ->
                    taskConfig.doFirst { executingTask ->
                        // Call static method to avoid capturing plugin instance
                        TaskActions.executeBuildIdInjectionFallback(
                            task = executingTask,
                            buildDir = buildDir,
                            variantName = variantName
                        )
                    }
                }
            }
        }
    }

    private fun setupUploadTask(
        variantName: String,
        applicationId: String,
        versionCode: Int,
        buildTypeName: String,
        buildDir: File,
        accessToken: String?,
        realm: String?,
        failBuildOnUploadFailure: Boolean
    ) {
        // Hook into the assemble task to upload after build completes
        val assembleTaskName = "assemble${variantName.capitalize(Locale.ROOT)}"
        logger.debug("Upload", "Hooking into task '$assembleTaskName' for upload")

        val assembleTask = project.tasks.named(assembleTaskName)
        assembleTask.configure { taskConfig ->
            taskConfig.doLast { executingTask ->
                // Call static method to avoid capturing plugin instance
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

    fun executeBuildIdInjection(task: Task, buildDir: File, variantName: String, manifestOutputFiles: Set<File>) {
        val taskLogger = SplunkLogger(task.logger)

        taskLogger.info("BuildId", "Executing injection after processManifest")

        val buildId = UUID.randomUUID().toString()
        taskLogger.lifecycle("BuildId", "Generated build ID for variant '$variantName': $buildId")

        BuildIdInjector.writeBuildIdToFile(buildDir, variantName, buildId)
        BuildIdInjector.injectMetadataIntoMergedManifest(variantName, manifestOutputFiles, buildId, taskLogger)
    }

    fun executeBuildIdInjectionFallback(task: Task, buildDir: File, variantName: String) {
        val taskLogger = SplunkLogger(task.logger)

        taskLogger.info("BuildId", "Executing injection via package task fallback")

        val buildId = UUID.randomUUID().toString()
        taskLogger.lifecycle("BuildId", "Generated build ID for variant '$variantName': $buildId")

        BuildIdInjector.writeBuildIdToFile(buildDir, variantName, buildId)

        val manifestFiles = BuildIdInjector.findManifestFiles(buildDir, variantName)
        BuildIdInjector.injectMetadataIntoManifestFiles(variantName, manifestFiles, buildId, taskLogger)
    }
}
