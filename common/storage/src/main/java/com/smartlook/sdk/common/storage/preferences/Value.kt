package com.smartlook.sdk.common.storage.preferences

import com.smartlook.sdk.common.utils.extensions.getFloat
import org.json.JSONObject
import kotlin.collections.component1
import kotlin.collections.component2

sealed interface Value

@JvmInline
value class StringValue(val value: String) : Value

@JvmInline
value class IntValue(val value: Int) : Value

@JvmInline
value class LongValue(val value: Long) : Value

@JvmInline
value class FloatValue(val value: Float) : Value

@JvmInline
value class BooleanValue(val value: Boolean) : Value

@JvmInline
value class StringMapValue(val value: Map<String, String>) : Value

fun serializeFromValueMap(map: HashMap<String, Value>): String {
    val json = JSONObject()
    for ((key, value) in map) {
        val valueObject = JSONObject()

        when (value) {
            is StringValue -> {
                valueObject.put("type", "String")
                valueObject.put("value", value.value)
            }

            is IntValue -> {
                valueObject.put("type", "Int")
                valueObject.put("value", value.value)
            }

            is LongValue -> {
                valueObject.put("type", "Long")
                valueObject.put("value", value.value)
            }

            is FloatValue -> {
                valueObject.put("type", "Float")
                valueObject.put("value", value.value)
            }

            is BooleanValue -> {
                valueObject.put("type", "Boolean")
                valueObject.put("value", value.value)
            }

            is StringMapValue -> {
                valueObject.put("type", "StringMap")
                valueObject.put("value", value.value)
            }
        }
        json.put(key, valueObject)
    }

    return json.toString()
}

fun deserializeAndFillMap(jsonString: String, map: HashMap<String, Value>) {
    val json = JSONObject(jsonString)

    for (key in json.keys()) {
        val valueObject = json.getJSONObject(key)
        val value = when (val valueType = valueObject.getString("type")) {
            "String" -> StringValue(valueObject.getString("value"))
            "Int" -> IntValue(valueObject.getInt("value"))
            "Long" -> LongValue(valueObject.getLong("value"))
            "Float" -> FloatValue(valueObject.getFloat("value"))
            "Boolean" -> BooleanValue(valueObject.getBoolean("value"))
            "StringMap" -> StringMapValue(valueObject.toMap())
            else -> throw IllegalArgumentException("Unsupported value type $valueType")
        }
        map[key] = value
    }
}

fun JSONObject.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    for (key in keys()) {
        val value = this.opt(key)
        if (value is String) {
            map[key] = value
        }
    }

    return map
}
