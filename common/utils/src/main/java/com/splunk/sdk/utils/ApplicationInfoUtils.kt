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

package com.splunk.sdk.utils

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.cisco.android.common.logger.Logger

class ApplicationInfoUtils {
    companion object {

        private const val TAG = "ErrorIdentifier"
        private const val SPLUNK_BUILD_ID = "splunk.build_id"

        fun retrieveApplicationId(application: Application): String? {
            val packageManager: PackageManager = application.packageManager
            val applicationInfo: ApplicationInfo?

            try {
                applicationInfo =
                    packageManager.getApplicationInfo(application.packageName, PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to retrieve ApplicationInfo: ${e.message}")
                return null
            }

            return applicationInfo.packageName ?: run {
                Logger.e(TAG, "ApplicationInfo is null, cannot extract applicationId")
                null
            }
        }

        fun retrieveVersionCode(application: Application): String? {
            val packageManager: PackageManager = application.packageManager

            return try {
                val packageInfo = packageManager.getPackageInfo(application.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toString()
                } else {
                    packageInfo.versionCode.toString()
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to get application version code", e)
                null
            }
        }

        fun retrieveSplunkBuildID(application: Application): String? {
            val packageManager: PackageManager = application.packageManager
            val applicationInfo: ApplicationInfo?

            try {
                applicationInfo =
                    packageManager.getApplicationInfo(application.packageName, PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to retrieve ApplicationInfo: ${e.message}")
                return null
            }

            val metadata = applicationInfo.metaData
            if (metadata == null) {
                Logger.d(TAG, "Application metadata bundle is null - no metadata present")
                return null
            }

            if (metadata.containsKey(SPLUNK_BUILD_ID)) {
                val value = metadata.get(SPLUNK_BUILD_ID)
                if (value == null) {
                    Logger.d(TAG, "Splunk Build ID exists but has null value")
                    return null
                }
                val buildId = value.toString()
                Logger.d(TAG, "Found Splunk Build ID: $buildId")
                return buildId
            } else {
                Logger.d(TAG, "Optional Splunk Build ID not found in metadata")
                return null
            }
        }
    }
}
