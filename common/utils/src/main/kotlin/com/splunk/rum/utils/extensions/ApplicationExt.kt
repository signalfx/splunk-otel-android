package com.splunk.rum.utils.extensions

import android.app.Application
import android.content.pm.PackageManager
import com.splunk.android.common.logger.Logger

private const val TAG = "ApplicationExt"
private const val SPLUNK_BUILD_ID = "splunk.build_id"

val Application.applicationId: String?
    get() {
        val id = try {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).packageName
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to retrieve ApplicationInfo: ${e.message}")
            null
        }
        return id
    }

val Application.splunkBuildId: String?
    get() {
        val appInfo = try {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to retrieve ApplicationInfo: ${e.message}")
            return null
        }

        val metadata = appInfo.metaData ?: run {
            Logger.d(TAG, "Application metadata bundle is null - no metadata present")
            return null
        }

        val buildId = if (metadata.containsKey(SPLUNK_BUILD_ID)) {
            val value = metadata.get(SPLUNK_BUILD_ID)
            if (value == null) {
                Logger.d(TAG, "Splunk Build ID exists but has null value")
                null
            } else {
                val id = value.toString()
                Logger.d(TAG, "Found Splunk Build ID: $id")
                id
            }
        } else {
            Logger.d(TAG, "Optional Splunk Build ID not found in metadata")
            null
        }

        return buildId
    }
