package com.smartlook.sdk.common.logger.extension

import android.util.Log

fun Int.toSeverityString(): String? {
    return when (this) {
        Log.VERBOSE -> "VERBOSE"
        Log.DEBUG -> "DEBUG"
        Log.INFO -> "INFO"
        Log.WARN -> "WARN"
        Log.ERROR -> "ERROR"
        Log.ASSERT -> "ASSERT"
        else -> null
    }
}
