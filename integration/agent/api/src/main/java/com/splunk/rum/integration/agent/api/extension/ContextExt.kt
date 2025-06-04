package com.splunk.rum.integration.agent.api.extension

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal const val FALLBACK_VERSION = "0.0.0"

internal val Context.appVersion: String
    get() = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            ).versionName
        } else {
            packageManager.getPackageInfo(packageName, 0).versionName
        }
    } catch (e: PackageManager.NameNotFoundException) {
        FALLBACK_VERSION
    }
