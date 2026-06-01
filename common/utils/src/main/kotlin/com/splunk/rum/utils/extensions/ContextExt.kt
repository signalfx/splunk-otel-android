package com.splunk.rum.utils.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.splunk.android.common.logger.Logger

private const val TAG = "ContextExt"

val Context.packageInfo: PackageInfo?
    get() = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to get PackageInfo", e)
        null
    }

val Context.appVersion: String?
    get() = packageInfo?.versionName

val Context.versionCode: String?
    get() = packageInfo?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            it.longVersionCode.toString()
        } else {
            it.versionCode.toString()
        }
    }
