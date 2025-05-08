/*
 * Copyright 2025 Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum.integration.agent.api.attributes

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import java.util.function.BiConsumer

/**
 * A utility class for managing custom RUM attributes.
 */
class MutableAttributes(
    @Volatile
    private var attributes: Attributes = Attributes.empty()
) : Attributes {

    /**
     * Retrieves the value associated with the given [AttributeKey].
     *
     * @param key the attribute key to retrieve
     * @return the value if present, or null
     */
    override operator fun <T> get(key: AttributeKey<T>): T? = key.let { attributes.get(it) }

    /**
     * Retrieves the value associated with the given key string.
     *
     * @param key the string key to retrieve
     * @return the value if present, or null
     * @throws ClassCastException if the stored value is not of the expected type
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String): T? =
        attributes.get(AttributeKey.stringKey(key)) as (T)

    /**
     * Sets a String value for the given key.
     *
     * @param key the key to set
     * @param value the value to associate
     */
    operator fun set(key: String, value: String) {
        attributes = attributes.edit { put(AttributeKey.stringKey(key), value) }
    }

    /**
     * Sets a Long value for the given key.
     *
     * @param key the key to set
     * @param value the value to associate
     */
    operator fun set(key: String, value: Long) {
        attributes = attributes.edit { put(AttributeKey.longKey(key), value) }
    }

    /**
     * Sets a Double value for the given key.
     *
     * @param key the key to set
     * @param value the value to associate
     */
    operator fun set(key: String, value: Double) {
        attributes = attributes.edit { put(AttributeKey.doubleKey(key), value) }
    }

    /**
     * Sets a Boolean value for the given key.
     *
     * @param key the key to set
     * @param value the value to associate
     */
    operator fun set(key: String, value: Boolean) {
        attributes = attributes.edit { put(AttributeKey.booleanKey(key), value) }
    }

    /**
     * Sets a value for a given [AttributeKey].
     *
     * @param key the attribute key
     * @param value the value to associate
     */
    operator fun <T : Any> set(key: AttributeKey<T>, value: T) {
        attributes = attributes.edit { put(key, value) }
    }

    /**
     * Removes the attribute associated with the given [AttributeKey].
     *
     * @param key the attribute key
     */
    fun <T> remove(key: AttributeKey<T>) {
        attributes = attributes.edit { remove(key) }
    }

    /**
     * Removes the attribute associated with the given key string.
     *
     * @param key the string key to remove
     */
    fun remove(key: String) {
        attributes = attributes.edit { removeIf { it.key == key } }
    }

    /**
     * Removes all attributes.
     */
    fun removeAll() {
        attributes = Attributes.empty()
    }

    /**
     * Sets multiple attributes at once from an existing [Attributes] instance.
     *
     * @param attributesToAdd the attributes to merge
     */
    fun setAll(attributesToAdd: Attributes) {
        attributes = attributes.edit { putAll(attributesToAdd) }
    }

    /**
     * Returns all stored attributes.
     *
     * @return the current attributes
     */
    fun getAll(): Attributes = attributes

    /**
     * Updates the attributes by applying the provided lambda to the current attributes.
     * This allows clients to modify the attributes in a flexible way.
     *
     * @param updateAttributes a lambda to modify the current attributes. The lambda receives an [AttributesBuilder].
     */
    fun update(updateAttributes: AttributesBuilder.() -> Unit) {
        attributes = attributes.edit(updateAttributes)
    }

    override fun forEach(consumer: BiConsumer<in AttributeKey<*>, in Any>) =
        attributes.forEach(consumer)

    override fun size(): Int = attributes.size()

    override fun isEmpty(): Boolean = attributes.isEmpty

    override fun asMap(): MutableMap<AttributeKey<*>, Any> = attributes.asMap()

    override fun toBuilder(): AttributesBuilder = attributes.toBuilder()

    private inline fun Attributes.edit(block: AttributesBuilder.() -> Unit): Attributes = toBuilder().apply(block).build()
}
