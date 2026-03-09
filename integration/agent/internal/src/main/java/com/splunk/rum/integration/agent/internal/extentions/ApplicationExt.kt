package com.splunk.rum.integration.agent.internal.extentions

import android.app.Application
import android.content.pm.PackageManager
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.agent.internal.RumConstants

private const val TAG = "ApplicationExt"

val Application.splunkBuildId: String?
    get() {
        val appInfo = try {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to retrieve ApplicationInfo", e)
            return null
        }

        val metadata = appInfo.metaData ?: run {
            Logger.d(TAG, "Application metadata bundle is null - no metadata present")
            return null
        }

        val buildId = if (metadata.containsKey(RumConstants.SPLUNK_BUILD_ID.key)) {
            val value = metadata.get(RumConstants.SPLUNK_BUILD_ID.key)
            if (value == null) {
                Logger.d(TAG, "Splunk Build ID exists but has null value")
                null
            } else {
                val id = value.toString()
                Logger.d(TAG) { "Found Splunk Build ID: $id" }
                id
            }
        } else {
            Logger.d(TAG, "Optional Splunk Build ID not found in metadata")
            null
        }

        return buildId
    }
