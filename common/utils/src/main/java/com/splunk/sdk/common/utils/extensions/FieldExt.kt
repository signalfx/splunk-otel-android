package com.splunk.sdk.common.utils.extensions

import android.annotation.SuppressLint
import android.os.Build
import java.lang.reflect.Field
import java.lang.reflect.Modifier

internal fun Field.makeReadable() {
    isAccessible = true
}

@SuppressLint("DiscouragedPrivateApi")
internal fun Field.makeWritable() {
    isAccessible = true

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val accessFlags = Field::class.java.getDeclaredField("accessFlags")
        accessFlags.isAccessible = true
        accessFlags.setInt(this, modifiers and Modifier.FINAL.inv())
    }
}
