package com.splunk.sdk.utils

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.cisco.android.common.logger.Logger

class ApplicationInfoUtils {
    companion object {

        private const val TAG = "ErrorIdentifier"
        private const val SPLUNK_UUID_MANIFEST_KEY = "SPLUNK_O11Y_CUSTOM_UUID"

        fun retrieveApplicationId(application: Application): String? {
            val packageManager: PackageManager = application.packageManager
            val applicationInfo: ApplicationInfo?

            try {
                applicationInfo = packageManager.getApplicationInfo(application.packageName, PackageManager.GET_META_DATA)
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

        fun retrieveCustomUUID(application: Application): String? {
            val packageManager: PackageManager = application.packageManager
            val applicationInfo: ApplicationInfo?

            try {
                applicationInfo = packageManager.getApplicationInfo(application.packageName, PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to retrieve ApplicationInfo: ${e.message}")
                return null
            }

            return applicationInfo.metaData?.getString(SPLUNK_UUID_MANIFEST_KEY)?.takeIf {
                it.isNotEmpty()
            } ?: run {
                Logger.e(TAG, "Application MetaData bundle is null or does not contain the UUID")
                null
            }
        }
    }
}
