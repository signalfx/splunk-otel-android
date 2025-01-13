package com.splunk.sdk.common.utils.extensions

import org.json.JSONObject

fun JSONObject.getFloat(name: String): Float {
    return getDouble(name).toFloat()
}

fun JSONObject.optFloatNull(name: String): Float? {
    this.optDouble(name).let { number ->
        return if (number.isNaN()) {
            null
        } else {
            number.toFloat()
        }
    }
}

fun JSONObject.optLongNull(name: String): Long? =
    this.optFloatNull(name)?.toLong()

fun JSONObject.optBooleanNull(name: String): Boolean? {
    return if (has(name)) {
        optBoolean(name)
    } else {
        null
    }
}

fun <R> JSONObject.map(transformer: (json: JSONObject, key: String) -> R): List<R> {
    val result = ArrayList<R>()

    for (key in keys())
        result += transformer(this, key)

    return result
}

fun <R> JSONObject.mapNotNull(transformer: (json: JSONObject, key: String) -> R?): List<R> {
    val result = ArrayList<R>()

    for (key in keys()) {
        result += transformer(this, key) ?: continue
    }

    return result
}
