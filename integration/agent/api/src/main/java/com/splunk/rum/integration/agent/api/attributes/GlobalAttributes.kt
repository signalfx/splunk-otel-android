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
        val prefixedKey = createPrefixedKey(key)
        return prefixedKey?.let { attributes.get(it) }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String): T? {
        val prefixedKey = prefixKey(key)
        return attributes.asMap().entries.firstOrNull { it.key.key == prefixedKey }?.value as? T
    }

    operator fun set(key: String, value: String) {
        val prefixedKey = prefixKey(key)
        val builder = attributes.toBuilder()
        builder.put(AttributeKey.stringKey(prefixedKey), value)
        attributes = builder.build()
    }

    operator fun set(key: String, value: Long) {
        val prefixedKey = prefixKey(key)
        val builder = attributes.toBuilder()
        builder.put(AttributeKey.longKey(prefixedKey), value)
        attributes = builder.build()
    }

    operator fun set(key: String, value: Double) {
        val prefixedKey = prefixKey(key)
        val builder = attributes.toBuilder()
        builder.put(AttributeKey.doubleKey(prefixedKey), value)
        attributes = builder.build()
    }

    operator fun set(key: String, value: Boolean) {
        val prefixedKey = prefixKey(key)
        val builder = attributes.toBuilder()
        builder.put(AttributeKey.booleanKey(prefixedKey), value)
        attributes = builder.build()
    }

    operator fun <T> set(key: AttributeKey<T>, value: T & Any) {
        val prefixedKey = createPrefixedKey(key)
        val builder = attributes.toBuilder()
        if (prefixedKey != null) {
            builder.put(prefixedKey, value)
        }
        attributes = builder.build()
    }

    fun <T> remove(key: AttributeKey<T>) {
        val prefixedKey = createPrefixedKey(key)
        val builder = attributes.toBuilder()
        if (prefixedKey != null) {
            builder.remove(prefixedKey)
        }
        attributes = builder.build()
    }

    fun remove(key: String) {
        val prefixedKey = prefixKey(key)
        val builder = attributes.toBuilder()
        builder.removeIf { it.key == prefixedKey }
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
    private fun <T> createPrefixedKey(key: AttributeKey<T>): AttributeKey<T>? {
        val prefixedKeyName = prefixKey(key.key)

        return when (key.type) {
            AttributeType.STRING -> AttributeKey.stringKey(prefixedKeyName) as AttributeKey<T>
            AttributeType.BOOLEAN -> AttributeKey.booleanKey(prefixedKeyName) as AttributeKey<T>
            AttributeType.LONG -> AttributeKey.longKey(prefixedKeyName) as AttributeKey<T>
            AttributeType.DOUBLE -> AttributeKey.doubleKey(prefixedKeyName) as AttributeKey<T>
            AttributeType.STRING_ARRAY -> AttributeKey.stringArrayKey(prefixedKeyName) as AttributeKey<T>
            AttributeType.BOOLEAN_ARRAY -> AttributeKey.booleanArrayKey(prefixedKeyName) as AttributeKey<T>
            AttributeType.LONG_ARRAY -> AttributeKey.longArrayKey(prefixedKeyName) as AttributeKey<T>
            AttributeType.DOUBLE_ARRAY -> AttributeKey.doubleArrayKey(prefixedKeyName) as AttributeKey<T>
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