package com.splunk.rum.integration.agent.api.attributes

import io.opentelemetry.api.common.AttributeKey

class GlobalAttributes private constructor() {

    companion object {
        val instance: GlobalAttributes by lazy { GlobalAttributes() }
    }

    private val _attributes: MutableList<Attribute> = ArrayList()
    val attributes: List<Attribute> get() = _attributes // Expose as immutable list

    // this method definitely can be streamlined
    operator fun set(key: Any, value: Any) {
        if (key is String) {
            val existingAttribute = _attributes.find { it.name == key }
            if (existingAttribute != null) {
                _attributes.remove(existingAttribute)
            }

            _attributes += when (value) {
                is String -> Attribute.String(key, value)
                is Boolean -> Attribute.Boolean(key, value)
                is Long -> Attribute.Long(key, value)
                is Double -> Attribute.Double(key, value)
                else -> throw IllegalArgumentException("Unsupported attribute type")
            }
        } else if (key is AttributeKey<*>) {
            val existingAttribute = _attributes.find { it.name == key.key }
            if (existingAttribute != null) {
                _attributes.remove(existingAttribute)
            }

            _attributes += when (value) {
                is String -> Attribute.String(key.key, value)
                is Boolean -> Attribute.Boolean(key.key, value)
                is Long -> Attribute.Long(key.key, value)
                is Double -> Attribute.Double(key.key, value)
                else -> throw IllegalArgumentException("Unsupported attribute type")
            }
        } else {
            throw IllegalArgumentException("Key must be either String or AttributeKey")
        }
    }

    fun remove(key: String) {
        _attributes.removeAll { it.name == key }
    }

    sealed interface Attribute {

        val name: kotlin.String

        data class Boolean(override val name: kotlin.String, val value: kotlin.Boolean) : Attribute
        data class Double(override val name: kotlin.String, val value: kotlin.Double) : Attribute
        data class String(override val name: kotlin.String, val value: kotlin.String) : Attribute
        data class Long(override val name: kotlin.String, val value: kotlin.Long) : Attribute
    }
}
