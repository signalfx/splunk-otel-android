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

package com.splunk.rum.integration.agent.common.attributes

import com.cisco.android.common.utils.extensions.forEachFast
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.Assert
import org.junit.Test

class MutableAttributesTest {

    @Test
    fun `get and set with String key`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes["key"] = "value"
        Assert.assertEquals("value", mutableAttributes["key"])
    }

    @Test
    fun `modify value with String key`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes["key"] = "value"
        Assert.assertEquals("value", mutableAttributes["key"])

        mutableAttributes["key"] = "value1"
        Assert.assertEquals("value1", mutableAttributes["key"])

        mutableAttributes["key"] = 0L
        Assert.assertEquals(0L, mutableAttributes["key"])
    }

    @Test
    fun `get and set with AttributeKey`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes[AttributeKey.stringKey("key")] = "value"
        Assert.assertEquals("value", mutableAttributes[AttributeKey.stringKey("key")])
    }

    @Test
    fun `get non-existent key returns null`() {
        val mutableAttributes = MutableAttributes()
        Assert.assertNull(mutableAttributes["key"])
        Assert.assertNull(mutableAttributes[AttributeKey.stringKey("key")])
    }

    @Test
    fun `set multiple types`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes["stringKey"] = "hello"
        mutableAttributes["longKey"] = 123L
        mutableAttributes["doubleKey"] = 45.67
        mutableAttributes["booleanKey"] = true

        Assert.assertEquals("hello", mutableAttributes["stringKey"])
        Assert.assertEquals(123L, mutableAttributes["longKey"])
        Assert.assertEquals(45.67, mutableAttributes["doubleKey"])
        Assert.assertEquals(true, mutableAttributes["booleanKey"])
    }

    @Test
    fun `remove with String key`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes["key"] = "value"
        Assert.assertEquals("value", mutableAttributes["key"])

        mutableAttributes.remove("key")
        Assert.assertNull(mutableAttributes["key"])
    }

    @Test
    fun `remove with AttributeKey`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes[AttributeKey.longKey("keyToRemove")] = 100L
        Assert.assertEquals(100L, mutableAttributes[AttributeKey.longKey("keyToRemove")])

        mutableAttributes.remove(AttributeKey.longKey("keyToRemove"))
        Assert.assertNull(mutableAttributes[AttributeKey.longKey("keyToRemove")])
    }

    @Test
    fun `removeAll clears all attributes`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes["key1"] = "value1"
        mutableAttributes["key2"] = 123L
        Assert.assertEquals(2, mutableAttributes.size())

        mutableAttributes.removeAll()

        Assert.assertTrue(mutableAttributes.isEmpty())
        Assert.assertEquals(0, mutableAttributes.size())
        Assert.assertNull(mutableAttributes["key1"])
        Assert.assertNull(mutableAttributes["key2"])
    }

    @Test
    fun `contains checks for key existence`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes["stringKey"] = "stringValue"
        mutableAttributes["longAttributeKey"] = 123L

        Assert.assertTrue("stringKey" in mutableAttributes)
        Assert.assertTrue("longAttributeKey" in mutableAttributes)
        Assert.assertTrue("nonExistentStringKey" !in mutableAttributes)

        mutableAttributes.remove("stringKey")
        Assert.assertTrue("stringKey" !in mutableAttributes)
        Assert.assertTrue("longAttributeKey" in mutableAttributes)
    }

    @Test
    fun `setAll adds attributes from another Attributes instance`() {
        val mutableAttributes = MutableAttributes()
        mutableAttributes["existingKey"] = "existingValue"

        val attributesToAdd = Attributes.builder()
            .put("newKey1", "newValue1")
            .put(AttributeKey.longKey("newKey2"), 99L)
            .build()

        mutableAttributes.setAll(attributesToAdd)

        Assert.assertEquals("existingValue", mutableAttributes["existingKey"])
        Assert.assertEquals("newValue1", mutableAttributes["newKey1"])
        Assert.assertEquals(99L, mutableAttributes[AttributeKey.longKey("newKey2")])
        Assert.assertEquals(3, mutableAttributes.size())
    }

    @Test
    fun `getAll returns current attributes`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes[AttributeKey.stringKey("key1")] = "value1"
        mutableAttributes["key2"] = true

        val allAttributes = mutableAttributes.getAll()
        Assert.assertEquals(2, allAttributes.size())
        Assert.assertEquals("value1", allAttributes[AttributeKey.stringKey("key1")])
        Assert.assertEquals(true, allAttributes[AttributeKey.booleanKey("key2")])
    }

    @Test
    fun `update modifies attributes`() {
        val mutableAttributes = MutableAttributes()
        mutableAttributes["initialKey"] = "initialValue"
        mutableAttributes["keyToUpdate"] = "value1"

        mutableAttributes.update {
            put("updatedKey", "updatedValue")
            remove(AttributeKey.stringKey("initialKey"))
            put("anotherKey", 12345L)
            put("keyToUpdate", "value2")
        }

        Assert.assertNull(mutableAttributes["initialKey"])
        Assert.assertEquals("updatedValue", mutableAttributes["updatedKey"])
        Assert.assertEquals("value2", mutableAttributes["keyToUpdate"])
        Assert.assertEquals(12345L, mutableAttributes["anotherKey"])
        Assert.assertEquals(3, mutableAttributes.size())
    }

    @Test
    fun `forEach iterates over attributes`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes[AttributeKey.stringKey("name")] = "test"
        mutableAttributes[AttributeKey.longKey("count")] = 10L

        val collectedAttributes = mutableMapOf<AttributeKey<*>, Any>()
        mutableAttributes.forEach { key, value -> collectedAttributes[key] = value }

        Assert.assertEquals(2, collectedAttributes.size)
        Assert.assertEquals("test", collectedAttributes[AttributeKey.stringKey("name")])
        Assert.assertEquals(10L, collectedAttributes[AttributeKey.longKey("count")])
    }

    @Test
    fun `size returns correct number of attributes`() {
        val mutableAttributes = MutableAttributes()
        Assert.assertEquals(0, mutableAttributes.size())
        mutableAttributes["a"] = "1"
        Assert.assertEquals(1, mutableAttributes.size())
        mutableAttributes["b"] = "2"
        Assert.assertEquals(2, mutableAttributes.size())
        mutableAttributes.remove("a")
        Assert.assertEquals(1, mutableAttributes.size())
    }

    @Test
    fun `isEmpty checks if attributes are empty`() {
        val mutableAttributes = MutableAttributes()
        Assert.assertTrue(mutableAttributes.isEmpty())
        mutableAttributes["a"] = "1"
        Assert.assertTrue(!mutableAttributes.isEmpty())
        mutableAttributes.removeAll()
        Assert.assertTrue(mutableAttributes.isEmpty())
    }

    @Test
    fun `asMap returns a map representation`() {
        val mutableAttributes = MutableAttributes()

        mutableAttributes[AttributeKey.stringKey("city")] = "Prague"
        mutableAttributes[AttributeKey.booleanKey("isActive")] = true

        val map = mutableAttributes.asMap()
        Assert.assertEquals(2, map.size)
        Assert.assertEquals("Prague", map[AttributeKey.stringKey("city")])
        Assert.assertEquals(true, map[AttributeKey.booleanKey("isActive")])
    }

    @Test
    fun `toBuilder creates a builder with current attributes`() {
        val mutableAttributes = MutableAttributes()
        mutableAttributes["name"] = "testBuilder"
        mutableAttributes["version"] = 1.0

        val builder = mutableAttributes.toBuilder()
        val newAttributes = builder.put("extra", true).build()

        Assert.assertEquals("testBuilder", newAttributes[AttributeKey.stringKey("name")])
        Assert.assertEquals(1.0, newAttributes[AttributeKey.doubleKey("version")])
        Assert.assertEquals(true, newAttributes[AttributeKey.booleanKey("extra")])
        Assert.assertNull(mutableAttributes["extra"])
        Assert.assertEquals(2, mutableAttributes.size())
    }

    @Test
    fun `thread safety test - concurrent writes and reads`() {
        val mutableAttributes = MutableAttributes()

        val numberOfThreads = 10
        val operationsPerThread = 100
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        val latch = CountDownLatch(numberOfThreads)

        val createKey = { threadIndex: Int, keyIndex: Int -> "thread_${threadIndex}_key_$keyIndex" }

        for (i in 0 until numberOfThreads) {
            executor.submit {
                try {
                    for (j in 0 until operationsPerThread) {
                        mutableAttributes[createKey(i, j)] = "value_$j"
                    }

                    for (j in 0 until operationsPerThread) {
                        Assert.assertEquals("value_$j", mutableAttributes[createKey(i, j)])
                    }

                    for (j in 0 until operationsPerThread) {
                        mutableAttributes.remove("thread_${i}_key_$j")
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        Assert.assertEquals(0, mutableAttributes.size())
    }

    @Test
    fun `thread safety test - concurrent setAll and removeAll`() {
        val mutableAttributes = MutableAttributes()
        val numberOfThreads = 100
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        val latch = CountDownLatch(numberOfThreads)

        val attributesList = ArrayList<Attributes>(numberOfThreads)

        for (i in 0 until numberOfThreads) {
            executor.submit {
                try {
                    val attributes = Attributes.builder()
                        .put("key_${i}_0", "value")
                        .put("key_${i}_1", 123)
                        .put("key_${i}_2", true)
                        .put("key_${i}_3", 123.45)
                        .build()

                    mutableAttributes.setAll(attributes)

                    if (i % 10 == 0) {
                        mutableAttributes.removeAll()
                        synchronized(attributesList) { attributesList.clear() }
                    } else {
                        synchronized(attributesList) { attributesList += attributes }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        val attributes = mutableAttributes.getAll()

        val allAttributes = Attributes.builder()
            .putAll(attributesList)
            .build()

        Assert.assertEquals(allAttributes, attributes)
    }

    private fun AttributesBuilder.putAll(list: Collection<Attributes>): AttributesBuilder {
        list.forEachFast { putAll(it) }
        return this
    }
}
