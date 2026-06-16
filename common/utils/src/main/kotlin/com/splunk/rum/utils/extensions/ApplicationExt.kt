package com.splunk.rum.utils.extensions

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.splunk.android.common.logger.Logger

private const val TAG = "ApplicationExt"

val Application.applicationId: String?
    get() {
        val id = try {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).packageName
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to retrieve ApplicationInfo", e)
            null
        }
        return id
    }

val Application.isStartedInForeground: Boolean
    get() {
        val myPid = Process.myPid()
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processInfo = activityManager.runningAppProcesses?.find { it.pid == myPid } ?: return false

        return processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }
