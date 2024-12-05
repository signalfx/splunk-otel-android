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

package com.cisco.android.rum.buildInfoInjector

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.cisco.android.rum.buildInfoInjector.utils.MappingFileModifier
import com.cisco.android.rum.buildInfoInjector.utils.ObfuscationTypeAnalyzer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

class BuildInfoInjectorPlugin : Plugin<Project>  {

    // TODO: ADD SONAR COVERAGE TO THIS AND UTILS CLASSES

    private val BUFFER_SIZE = 32_768

    override fun apply(project: Project) {

        val extension = project.extensions.getByName("androidComponents") as ApplicationAndroidComponentsExtension
        extension.onVariants { variant ->
            project.afterEvaluate {
                executePluginTask(project,  variant)
            }
        }
    }

    fun executePluginTask(
        project: Project,
        variant: ApplicationVariant
    ) {
        // Determine obfuscation type, returns "No Obfuscation" if obfuscation disabled
        val obfuscationType =
            variant.buildType?.let {
                ObfuscationTypeAnalyzer().determineObfuscationType(project,
                    it
                )
            }

        val packageTask = project.tasks.findByName("package${variant.name.capitalize(Locale.ROOT)}")

        // Task to append buildInfo to mapping files
        val appendBuildInfoToMappingFileTask = project.tasks.register("appendBuildInfoToMappingFileTask${variant.name.capitalize(Locale.ROOT)}") {
            doLast {
                try {
                    if (obfuscationType != null && !obfuscationType.equals(ObfuscationTypeAnalyzer.ObfuscationType.NO_OBFUSCATION.toString())) {
                        // Must get hash here after packageTask, after apk is generated
                        val apkFile = obtainApkFile(project, logger, variant.name, variant.buildType, variant.flavorName)
                        val hash =
                            apkFile?.let { calculateSha256(it) }

                        MappingFileModifier(logger).appendBuildInfoToMappingFile(project, obfuscationType, hash, variant)
                    }
                } catch (e: Exception) {
                    logger.error("Exception encountered while adding build info to mapping file: $e")
                }
            }
        }

        // Must run after package tasks to ensure apk and mapping files are generated
        packageTask?.finalizedBy(appendBuildInfoToMappingFileTask)
    }

    private fun obtainApkFile(project: Project,  logger: Logger, variantName: String, buildTypeName: String?, flavorName: String?): File? {
        if (buildTypeName == null) {
            logger.error("APK file is needed to calculate build info," +
                    " unable to determine the buildType and apk directory for build variant: $variantName" )
        }
        var apkDir = project.file("${project.buildDir}/outputs/apk/$buildTypeName")
        if (flavorName != null) {
            apkDir = project.file("${project.buildDir}/outputs/apk/$flavorName/$buildTypeName")
        }

        val apkFiles = apkDir.listFiles(File::isFile)?.filter { it.name.endsWith(".apk") }

        if (apkFiles.isNullOrEmpty()) {
            logger.error("APK file is needed to calculate build info but it could not be found" +
                    " for variant $variantName (buildType: $buildTypeName, flavor: $flavorName). Make sure the app has been assembled.")
            return null
        }

        return apkFiles.maxByOrNull { it.lastModified() } ?: run {
            logger.error("APK file is needed to calculate build info," +
                    " unable to determine the most recently generated APK file for variant $variantName (buildType: $buildTypeName, flavor: $flavorName).")
            return null
        }
    }

    private fun calculateSha256(file: File): String {
        // Reading apk file in chunks at a time

        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        val inputStream = FileInputStream(file)

        try {
            var bytesRead = inputStream.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }
        } finally {
            inputStream.close()
        }

        val hashBytes = digest.digest()

        // Convert byte array to lower case hexadecimal string
        val hexString = StringBuilder()
        for (byte in hashBytes) {
            hexString.append(String.format("%02x", byte))
        }
        return hexString.toString()
    }
}