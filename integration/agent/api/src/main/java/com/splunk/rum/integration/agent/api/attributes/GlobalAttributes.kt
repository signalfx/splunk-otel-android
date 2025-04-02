package com.splunk.rum.integration.agent.api.attributes

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType
import io.opentelemetry.api.common.Attributes
import com.cisco.android.common.logger.Logger

class GlobalAttributes constructor(
    @Volatile
    private var attributes: Attributes = Attributes.empty()
) {
    companion object {
        val instance: GlobalAttributes by lazy { GlobalAttributes() }
        private const val TAG = "GlobalAttributes"
        private const val CUSTOM_PREFIX = "custom."
    }

    operator fun <T> get(key: AttributeKey<T>): T? {
        val prefixedAttributeKey = createPrefixedAttributeKey(key)
        return prefixedAttributeKey?.let { attributes.get(it) }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String): T? {
        val prefixedAttributeKey = prefixKey(key)
        return attributes.asMap().entries.firstOrNull { it.key.key == prefixedAttributeKey }?.value as? T
    }

    operator fun set(key: String, value: String) {
        val prefixedAttributeKey = prefixKey(key)
        val builder = attributes.toBuilder()
        builder.put(AttributeKey.stringKey(prefixedAttributeKey), value)
        attributes = builder.build()
    }

    operator fun set(key: String, value: Long) {
        val prefixedAttributeKey = prefixKey(key)
        val builder = attributes.toBuilder()
        builder.put(AttributeKey.longKey(prefixedAttributeKey), value)
        attributes = builder.build()
    }

    operator fun set(key: String, value: Double) {
        val prefixedAttributeKey = prefixKey(key)
        val builder = attributes.toBuilder()
        builder.put(AttributeKey.doubleKey(prefixedAttributeKey), value)
        attributes = builder.build()
    }

    operator fun set(key: String, value: Boolean) {
        val prefixedAttributeKey = prefixKey(key)
        val builder = attributes.toBuilder()
        builder.put(AttributeKey.booleanKey(prefixedAttributeKey), value)
        attributes = builder.build()
    }

    operator fun <T> set(key: AttributeKey<T>, value: T & Any) {
        val prefixedAttributeKey = createPrefixedAttributeKey(key)
        val builder = attributes.toBuilder()
        if (prefixedAttributeKey != null) {
            builder.put(prefixedAttributeKey, value)
        }
        attributes = builder.build()
    }

    fun <T> remove(key: AttributeKey<T>) {
        val prefixedAttributeKey = createPrefixedAttributeKey(key)
        val builder = attributes.toBuilder()
        if (prefixedAttributeKey != null) {
            builder.remove(prefixedAttributeKey)
        }
        attributes = builder.build()
    }

    fun remove(key: String) {
        val prefixedAttributeKey = prefixKey(key)
        val builder = attributes.toBuilder()
        builder.removeIf { it.key == prefixedAttributeKey }
        attributes = builder.build()
    }

    fun removeAll() {
        attributes = Attributes.empty()
    }

    fun setAll(newAttributes: Attributes) {
        newAttributes.forEach { key, value ->
            @Suppress("UNCHECKED_CAST")
            set(key as AttributeKey<Any>, value)
        }
    }

    fun getAll(): Attributes = attributes

    private fun prefixKey(key: String): String = CUSTOM_PREFIX + key

    @Suppress("UNCHECKED_CAST")
    private fun <T> createPrefixedAttributeKey(key: AttributeKey<T>): AttributeKey<T>? {
        val prefixedAttributeKeyName = prefixKey(key.key)

        return when (key.type) {
            AttributeType.STRING -> AttributeKey.stringKey(prefixedAttributeKeyName) as AttributeKey<T>
            AttributeType.BOOLEAN -> AttributeKey.booleanKey(prefixedAttributeKeyName) as AttributeKey<T>
            AttributeType.LONG -> AttributeKey.longKey(prefixedAttributeKeyName) as AttributeKey<T>
            AttributeType.DOUBLE -> AttributeKey.doubleKey(prefixedAttributeKeyName) as AttributeKey<T>
            AttributeType.STRING_ARRAY -> AttributeKey.stringArrayKey(prefixedAttributeKeyName) as AttributeKey<T>
            AttributeType.BOOLEAN_ARRAY -> AttributeKey.booleanArrayKey(prefixedAttributeKeyName) as AttributeKey<T>
            AttributeType.LONG_ARRAY -> AttributeKey.longArrayKey(prefixedAttributeKeyName) as AttributeKey<T>
            AttributeType.DOUBLE_ARRAY -> AttributeKey.doubleArrayKey(prefixedAttributeKeyName) as AttributeKey<T>
            null -> {
                Logger.d(TAG, "Null attribute type encountered")
                null
            }
            else -> {
                Logger.d(TAG, "Unsupported attribute type: ${key.type}")
                null
            }
        }
    }
}