package com.splunk.sdk.common.utils.extensions

import org.json.JSONArray

fun <T> JSONArray.map(transformation: (array: JSONArray, index: Int) -> T): List<T> {
    val result = ArrayList<T>()

    for (i in 0 until length())
        result += transformation(this, i)

    return result
}
