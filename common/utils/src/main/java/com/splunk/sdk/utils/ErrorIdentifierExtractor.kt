package com.splunk.sdk.utils

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

class ErrorIdentifierExtractor(private val application: Application) {
    private val packageManager: PackageManager = application.packageManager
    private val applicationInfo: ApplicationInfo?

    init {
        var appInfo: ApplicationInfo?
        try {
            appInfo =
                packageManager.getApplicationInfo(
                    application.packageName, PackageManager.GET_META_DATA
                )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to initialize ErrorIdentifierExtractor: " + e.message
            )
            appInfo = null
        }
        this.applicationInfo = appInfo
    }

    fun extractInfo(): ErrorIdentifierInfo {
        var applicationId: String? = null
        val versionCode = retrieveVersionCode()
        val customUUID = retrieveCustomUUID()

        if (applicationInfo != null) {
            applicationId = applicationInfo.packageName
        } else {
            Log.e(TAG, "ApplicationInfo is null, cannot extract applicationId")
        }

        return ErrorIdentifierInfo(applicationId, versionCode, customUUID)
    }

    private fun retrieveVersionCode(): String? {
        try {
            val packageInfo =
                packageManager.getPackageInfo(application.packageName, 0)
            return packageInfo.versionCode.toString()
//            return packageInfo.longVersionCode.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get application version code", e)
            return null
        }
    }

    private fun retrieveCustomUUID(): String? {
        if (applicationInfo == null) {
            Log.e(TAG, "ApplicationInfo is null; cannot retrieve Custom UUID.")
            return null
        }
        val bundle = applicationInfo.metaData
        if (bundle != null) {
            return bundle.getString(SPLUNK_UUID_MANIFEST_KEY)
        } else {
            Log.e(TAG, "Application MetaData bundle is null")
            return null
        }
    }

    companion object {
        private const val TAG = "ErrorIdentifier"

        private const val SPLUNK_UUID_MANIFEST_KEY = "SPLUNK_O11Y_CUSTOM_UUID"
    }
}
