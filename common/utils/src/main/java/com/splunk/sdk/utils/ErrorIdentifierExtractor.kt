package com.splunk.sdk.utils

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.cisco.android.common.logger.Logger

class ErrorIdentifierExtractor(private val application: Application) {
    private val packageManager: PackageManager = application.packageManager
    private val applicationInfo: ApplicationInfo?

    init {
        applicationInfo = try {
            packageManager.getApplicationInfo(application.packageName, PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize ErrorIdentifierExtractor: ${e.message}")
            null
        }
    }

    fun extractInfo(): ErrorIdentifierInfo {
        var applicationId: String? = null
        val versionCode = retrieveVersionCode()
        val customUUID = retrieveCustomUUID()

        if (applicationInfo != null) {
            applicationId = applicationInfo.packageName
        } else {
            Logger.e(TAG, "ApplicationInfo is null, cannot extract applicationId")
        }

        return ErrorIdentifierInfo(applicationId, versionCode, customUUID)
    }

    private fun retrieveVersionCode(): String? {
        try {
            val packageInfo =
                packageManager.getPackageInfo(application.packageName, 0)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                packageInfo.versionCode.toString()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get application version code", e)
            return null
        }
    }

    private fun retrieveCustomUUID(): String? {
        if (applicationInfo == null) {
            Logger.e(TAG, "ApplicationInfo is null; cannot retrieve Custom UUID.")
            return null
        }
        val bundle = applicationInfo.metaData
        if (bundle != null) {
            return bundle.getString(SPLUNK_UUID_MANIFEST_KEY)
        } else {
            Logger.e(TAG, "Application MetaData bundle is null")
            return null
        }
    }

    companion object {
        private const val TAG = "ErrorIdentifier"

        private const val SPLUNK_UUID_MANIFEST_KEY = "SPLUNK_O11Y_CUSTOM_UUID"
    }
}
