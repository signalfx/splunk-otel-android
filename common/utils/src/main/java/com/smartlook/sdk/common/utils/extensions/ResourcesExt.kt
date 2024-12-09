package com.smartlook.sdk.common.utils.extensions

import android.content.res.Resources
import android.util.TypedValue

fun Resources.getValue(name: String, resolveRefs: Boolean = false): TypedValue {
    val value = TypedValue()
    getValue(name, value, resolveRefs)
    return value
}
