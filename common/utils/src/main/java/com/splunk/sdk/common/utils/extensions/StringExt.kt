package com.splunk.sdk.common.utils.extensions

import org.json.JSONArray
import org.json.JSONObject
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

fun String.toKClass(): KClass<*>? {
    return try {
        val clazz = Class.forName(this)
        Reflection.createKotlinClass(clazz)
    } catch (e: Exception) {
        null
    }
}

fun String.toJSONObject(): JSONObject {
    return JSONObject(this)
}

fun String.toJSONArray(): JSONArray {
    return JSONArray(this)
}
