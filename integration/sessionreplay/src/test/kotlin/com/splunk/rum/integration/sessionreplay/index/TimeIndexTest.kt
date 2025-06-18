package com.splunk.rum.integration.sessionreplay.index

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TimeIndexTest {
    @Test
    fun `putAt stores value at correct time`() {
        val index = TimeIndex<String>()
        val time = Instant.parse("2025-06-18T10:00:00Z")

        index.putAt(time, "Value A")

        assertEquals("Value A", index.getAt(time))
    }

    @Test
    fun `getAt returns closest previous value`() {
        val index = TimeIndex<String>()
        val t1 = Instant.parse("2025-06-18T10:00:00Z")
        val t2 = Instant.parse("2025-06-18T12:00:00Z")

        index.putAt(t1, "Value A")
        index.putAt(t2, "Value B")

        val queryTime = Instant.parse("2025-06-18T11:00:00Z")
        assertEquals("Value A", index.getAt(queryTime))
    }

    @Test
    fun `getAt returns null if no previous value`() {
        val index = TimeIndex<String>()
        val futureTime = Instant.parse("2025-06-18T10:00:00Z")

        assertNull(index.getAt(futureTime))
    }

    @Test
    fun `put stores value with current time`() {
        val index = TimeIndex<String>()
        index.put("NowValue")

        val now = Instant.now()
        val retrieved = index.getAt(now)

        assertNotNull(retrieved)
        assertEquals("NowValue", retrieved)
    }
}
