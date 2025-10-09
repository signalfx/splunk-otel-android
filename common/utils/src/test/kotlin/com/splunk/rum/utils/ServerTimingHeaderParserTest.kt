package com.splunk.rum.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerTimingHeaderParserTest {

    @Test
    fun `parse valid header with double quotes`() {
        val header = """traceparent;desc="00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01""""
        val expected = ServerTraceContext("9499195c502eb217c448a68bfe0f967c", "fe16eca542cd5d86")
        assertEquals(expected, ServerTimingHeaderParser.parse(header))
    }

    @Test
    fun `parse valid header with single quotes`() {
        val header = """traceparent;desc='00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01'"""
        val expected = ServerTraceContext("9499195c502eb217c448a68bfe0f967c", "fe16eca542cd5d86")
        assertEquals(expected, ServerTimingHeaderParser.parse(header))
    }

    @Test
    fun `parse invalid header - incorrect format`() {
        val header = "invalid-header-format"
        assertNull(ServerTimingHeaderParser.parse(header))
    }

    @Test
    fun `parse invalid header - missing traceparent`() {
        val header = """desc="00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01""""
        assertNull(ServerTimingHeaderParser.parse(header))
    }

    @Test
    fun `parse invalid header - missing desc`() {
        val header = """traceparent;="00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01""""
        assertNull(ServerTimingHeaderParser.parse(header))
    }

    @Test
    fun `parse invalid header - incorrect traceId format`() {
        val header = """traceparent;desc="00-INVALIDTRACEID-fe16eca542cd5d86-01""""
        assertNull(ServerTimingHeaderParser.parse(header))
    }

    @Test
    fun `parse invalid header - incorrect spanId format`() {
        val header = """traceparent;desc="00-9499195c502eb217c448a68bfe0f967c-INVALIDSPANID-01""""
        assertNull(ServerTimingHeaderParser.parse(header))
    }

    @Test
    fun `parse null header`() {
        assertNull(ServerTimingHeaderParser.parse(null))
    }

    @Test
    fun `parse empty header`() {
        assertNull(ServerTimingHeaderParser.parse(""))
    }

    @Test
    fun `parse blank header`() {
        assertNull(ServerTimingHeaderParser.parse("   "))
    }
}
