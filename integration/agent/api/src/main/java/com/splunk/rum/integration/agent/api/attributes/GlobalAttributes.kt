package com.splunk.rum.integration.agent.api.attributes

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.ConcurrentHashMap

class GlobalAttributes private constructor() {

    companion object {
        val instance: GlobalAttributes by lazy { GlobalAttributes() }
        private const val CUSTOM_PREFIX = "custom."
    }

    private val _attributes = ConcurrentHashMap<String, Attribute>()

    // Expose as read-only map view to prevent external modification
    val attributes: Map<String, Attribute> get() = _attributes

    operator fun <T> get(key: AttributeKey<T>): T? {
        val prefixedKey = CUSTOM_PREFIX + key.key
        val attribute = _attributes[prefixedKey] ?: return null

        @Suppress("UNCHECKED_CAST")
        return when (attribute) {
            is Attribute.Boolean -> attribute.value as? T
            is Attribute.Double -> attribute.value as? T
            is Attribute.String -> attribute.value as? T
            is Attribute.Long -> attribute.value as? T
        }
    }

    operator fun get(key: String): Any? {
        val prefixedKey = CUSTOM_PREFIX + key
        val attribute = _attributes[prefixedKey] ?: return null

        return when (attribute) {
            is Attribute.Boolean -> attribute.value
            is Attribute.Double -> attribute.value
            is Attribute.String -> attribute.value
            is Attribute.Long -> attribute.value
        }
    }

    operator fun set(key: String, value: String) {
        val prefixedKey = CUSTOM_PREFIX + key
        _attributes[prefixedKey] = Attribute.String(prefixedKey, value)
    }

    operator fun set(key: String, value: Long) {
        val prefixedKey = CUSTOM_PREFIX + key
        _attributes[prefixedKey] = Attribute.Long(prefixedKey, value)
    }

    operator fun set(key: String, value: Double) {
        val prefixedKey = CUSTOM_PREFIX + key
        _attributes[prefixedKey] = Attribute.Double(prefixedKey, value)
    }

    operator fun set(key: String, value: Boolean) {
        val prefixedKey = CUSTOM_PREFIX + key
        _attributes[prefixedKey] = Attribute.Boolean(prefixedKey, value)
    }

    operator fun <T> set(key: AttributeKey<T>, value: String) {
        val prefixedKey = CUSTOM_PREFIX + key.key
        _attributes[prefixedKey] = Attribute.String(prefixedKey, value)
    }

    operator fun <T> set(key: AttributeKey<T>, value: Long) {
        val prefixedKey = CUSTOM_PREFIX + key.key
        _attributes[prefixedKey] = Attribute.Long(prefixedKey, value)
    }

    operator fun <T> set(key: AttributeKey<T>, value: Double) {
        val prefixedKey = CUSTOM_PREFIX + key.key
        _attributes[prefixedKey] = Attribute.Double(prefixedKey, value)
    }

    operator fun <T> set(key: AttributeKey<T>, value: Boolean) {
        val prefixedKey = CUSTOM_PREFIX + key.key
        _attributes[prefixedKey] = Attribute.Boolean(prefixedKey, value)
    }

    fun <T> remove(key: AttributeKey<T>) {
        val prefixedKey = CUSTOM_PREFIX + key.key
        _attributes.remove(prefixedKey)
    }

    fun remove(key: String) {
        val prefixedKey = CUSTOM_PREFIX + key
        _attributes.remove(prefixedKey)
    }

    fun removeAll() {
        _attributes.clear()
    }

    fun setAll(attributes: Attributes) {
        attributes.forEach { key, value ->
            when (value) {
                is Boolean -> set(key, value)
                is String -> set(key, value)
                is Long -> set(key, value)
                is Double -> set(key, value)
                else -> throw IllegalArgumentException("Unsupported attribute type")
            }
        }
    }

    fun getAll(): Attributes {
        val builder = Attributes.builder()

        _attributes.forEach { (_, attribute) ->
            when (attribute) {
                is Attribute.Boolean -> builder.put(attribute.name, attribute.value)
                is Attribute.Double -> builder.put(attribute.name, attribute.value)
                is Attribute.String -> builder.put(attribute.name, attribute.value)
                is Attribute.Long -> builder.put(attribute.name, attribute.value)
            }
        }
        return builder.build()
    }


    sealed interface Attribute {
        val name: kotlin.String

        data class Boolean(override val name: kotlin.String, val value: kotlin.Boolean) : Attribute
        data class Double(override val name: kotlin.String, val value: kotlin.Double) : Attribute
        data class String(override val name: kotlin.String, val value: kotlin.String) : Attribute
        data class Long(override val name: kotlin.String, val value: kotlin.Long) : Attribute
    }
}