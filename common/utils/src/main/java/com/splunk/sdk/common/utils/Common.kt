package com.splunk.sdk.common.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Perform [block] when Android version is at least [versionCode].
 *
 * @param versionCode Android SDK version code from [Build.VERSION_CODES]
 */
@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
inline fun <T> runOnAndroidAtLeast(versionCode: Int, crossinline block: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= versionCode) block() else null
}
