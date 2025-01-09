/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.rum.library.httpurlconnection

import android.os.SystemClock
import android.util.Log
import com.splunk.rum.library.common.HttpConfigUtil
import com.splunk.rum.library.httpurlconnection.HttpUrlReplacements
import com.splunk.rum.library.httpurlconnection.tracing.HttpUrlConnectionSingletons
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URLConnection

class HttpUrlReplacementsTest {

    @MockK
    private lateinit var mockInstrumenter: Instrumenter<URLConnection, Int>

    @MockK
    private lateinit var mockUrlConnection: URLConnection

    @MockK
    private lateinit var mockParentContext: Context

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockInputStream: InputStream

    @MockK
    private lateinit var mockHttpURLConnection: HttpURLConnection

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkStatic(HttpUrlReplacements::class)
        HttpUrlReplacements.activeURLConnections.clear()

        mockkStatic(Context::class)
        every { Context.current() } returns mockParentContext

        mockkStatic(HttpConfigUtil::class)

        mockkStatic(HttpUrlConnectionSingletons::class)

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.d(any(), any(), any()) } returns 0

        mockkStatic(SystemClock::class)
        every { SystemClock.uptimeMillis() } returns 123456789L
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startTracingAtFirstConnection should add connection to map and call instrumenter start when all conditions are favorable`() {
        //Given
        every { HttpConfigUtil.isNetworkTracingEnabled() } returns true
        every { HttpUrlConnectionSingletons.instrumenter() } returns mockInstrumenter
        every { mockInstrumenter.shouldStart(mockParentContext, mockUrlConnection) } returns true
        every { mockInstrumenter.start(mockParentContext, mockUrlConnection) } returns mockContext
        every { HttpUrlReplacements.injectContextToRequest(any(), any()) } just Runs

        //When
        HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection)

        //Then
        verify { mockInstrumenter.start(mockParentContext, mockUrlConnection) }
        assertTrue(HttpUrlReplacements.activeURLConnections.containsKey(mockUrlConnection))
        verify { HttpUrlReplacements.injectContextToRequest(mockUrlConnection, mockContext) }
    }

    @Test
    fun `startTracingAtFirstConnection should log and should not call intrumenter start method when network tracing is disabled`() {
        //Given
        every { HttpConfigUtil.isNetworkTracingEnabled() } returns false

        //When
        HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection)

        //Then
        verify { Log.d(any(), "Network tracing has been disabled.") }
        verify(exactly = 0) { mockInstrumenter.start(any(), any()) }
    }

    @Test
    fun `startTracingAtFirstConnection should log and should not call intrumenter start method when instrumenter is null`() {
        //Given
        every { HttpConfigUtil.isNetworkTracingEnabled() } returns true
        every { HttpUrlConnectionSingletons.instrumenter() } returns null

        //When
        HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection)

        //Then
        verify { Log.d(any(), "Instrumenter is null.") }
        verify(exactly = 0) { mockInstrumenter.start(any(), any()) }
    }

    @Test
    fun `startTracingAtFirstConnection should not call intrumenter start method when instrumenter shouldStart returns false`() {
        //Given
        every { HttpConfigUtil.isNetworkTracingEnabled() } returns true
        every { HttpUrlConnectionSingletons.instrumenter() } returns mockInstrumenter
        every { mockInstrumenter.shouldStart(mockParentContext, mockUrlConnection) } returns false

        //When
        HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection)

        //Then
        verify(exactly = 0) { mockInstrumenter.start(any(), any()) }
    }

    @Test
    fun `startTracingAtFirstConnection logs an exception when injectContextToRequest throws exception`() {
        //Given
        val expectedTag = "HttpUrlReplacements"
        val expectedMessage =
            "An exception was thrown while adding distributed tracing context for connection "
        val expectedException = IllegalStateException("Test exception")
        every { HttpConfigUtil.isNetworkTracingEnabled() } returns true
        every { HttpUrlConnectionSingletons.instrumenter() } returns mockInstrumenter
        every { mockInstrumenter.shouldStart(mockParentContext, mockUrlConnection) } returns true
        every { mockInstrumenter.start(mockParentContext, mockUrlConnection) } returns mockContext
        every {
            HttpUrlReplacements.injectContextToRequest(
                mockUrlConnection,
                mockContext
            )
        } throws expectedException

        //When
        HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection)

        //Then
        verify(exactly = 1) {
            Log.d(expectedTag, expectedMessage + mockUrlConnection.toString(), expectedException)
        }
    }

    @Test
    fun `endTracing should call instrumenter end and remove connection from map when entry exists and not reported`() {
        //Given
        val responseCode = 200
        val error: Throwable? = null

        every { HttpUrlConnectionSingletons.instrumenter() } returns mockInstrumenter
        every {
            mockInstrumenter.end(
                mockContext,
                mockUrlConnection,
                responseCode,
                error
            )
        } returns Unit

        val httpURLConnectionInfo = getMockHttpURLConnectionInfo(mockUrlConnection)

        //When
        HttpUrlReplacements.endTracing(mockUrlConnection, responseCode, error)

        //Then
        verifyOrder {
            mockInstrumenter.end(mockContext, mockUrlConnection, responseCode, error)
            HttpUrlReplacements.activeURLConnections.remove(mockUrlConnection)
        }

        assertTrue(httpURLConnectionInfo.reported)
        assertFalse(HttpUrlReplacements.activeURLConnections.containsKey(mockUrlConnection))
    }

    @Test
    fun `endTracing should not call instrumenter end when connection does not exist in map`() {
        //Given
        val responseCode = 404
        val error: Throwable = Exception("Not Found")

        //When
        HttpUrlReplacements.endTracing(mockUrlConnection, responseCode, error)

        //Then
        verify(exactly = 0) { mockInstrumenter.end(any(), any(), any(), any()) }
    }

    @Test
    fun `endTracing should not call instrumenter end when connection is already reported`() {
        //Given
        val responseCode = 500
        val error: Throwable = Exception("Server Error")

        val httpURLConnectionInfo = getMockHttpURLConnectionInfo(mockUrlConnection)

        httpURLConnectionInfo.reported = true

        //When
        HttpUrlReplacements.endTracing(mockUrlConnection, responseCode, error)

        //Then
        verify(exactly = 0) { mockInstrumenter.end(any(), any(), any(), any()) }
    }

    @Test
    fun `injectContextToRequest should call textMapPropagator inject`() {

        val openTelemetrySdk: OpenTelemetrySdk = mockk()
        val textMapPropagator: TextMapPropagator = mockk()

        every { HttpUrlConnectionSingletons.openTelemetrySdkInstance() } returns openTelemetrySdk
        every { openTelemetrySdk.propagators.textMapPropagator } returns textMapPropagator
        every { textMapPropagator.inject(mockContext, mockUrlConnection, any()) } returns Unit

        HttpUrlReplacements.injectContextToRequest(mockUrlConnection, mockContext)

        verify { textMapPropagator.inject(mockContext, mockUrlConnection, any()) }
    }

    @Test
    fun `updateLastSeenTime should update last seen time for unreported connections`() {

        // Given
        val httpURLConnectionInfo = getMockHttpURLConnectionInfo(mockUrlConnection)

        httpURLConnectionInfo.reported = false

        httpURLConnectionInfo.lastSeenTime = 0L

        // When
        HttpUrlReplacements.updateLastSeenTime(mockUrlConnection)

        // Then
        assert(httpURLConnectionInfo.lastSeenTime == 123456789L)
    }

    @Test
    fun `updateLastSeenTime should not update last seen time for reported connections`() {
        // Given
        val httpURLConnectionInfo = getMockHttpURLConnectionInfo(mockUrlConnection)

        httpURLConnectionInfo.reported = true

        httpURLConnectionInfo.lastSeenTime = 0L

        // When
        HttpUrlReplacements.updateLastSeenTime(mockUrlConnection)

        // Then
        assert(httpURLConnectionInfo.lastSeenTime == 0L)
    }

    @Test
    fun `markHarvestable should mark connection as harvestable for unreported connections`() {
        // Given
        val httpURLConnectionInfo = getMockHttpURLConnectionInfo(mockUrlConnection)

        httpURLConnectionInfo.reported = false

        // When
        HttpUrlReplacements.markHarvestable(mockUrlConnection)

        // Then
        assert(httpURLConnectionInfo.harvestable)
    }

    @Test
    fun `markHarvestable should not mark connection as harvestable for reported connections`() {
        // Given
        val httpURLConnectionInfo = getMockHttpURLConnectionInfo(mockUrlConnection)

        httpURLConnectionInfo.reported = true

        // When
        HttpUrlReplacements.markHarvestable(mockUrlConnection)

        // Then
        assert(!httpURLConnectionInfo.harvestable)
    }

    @Test
    fun `replace should correctly fetch resultProvider get() value and should call all relevant methods`() {
        // Given
        val mockResultProvider: HttpUrlReplacements.ResultProvider<String> = mockk()
        every { mockResultProvider.get() } returns "result"

        // When
        val result = HttpUrlReplacements.replace(mockUrlConnection, mockResultProvider)

        // Then
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
        assertEquals("result", result)
    }

    @Test(expected = IOException::class)
    fun `replaceThrowable should call reportWithThrowable if there is an IOException`() {
        // Given
        val mockThrowableResultProvider: HttpUrlReplacements.ThrowableResultProvider<String> =
            mockk()
        every { mockThrowableResultProvider.get() } throws IOException()

        try {
            // When
            HttpUrlReplacements.replaceThrowable(
                mockUrlConnection,
                mockThrowableResultProvider,
                true
            )
        } catch (e: IOException) {
            //Then
            verify { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) }
            verify { HttpUrlReplacements.reportWithThrowable(mockUrlConnection, e) }
            verify(exactly = 0) { HttpUrlReplacements.updateLastSeenTime(mockUrlConnection) }
            verify(exactly = 0) { HttpUrlReplacements.markHarvestable(mockUrlConnection) }
            throw e
        }
    }

    @Test
    fun `replaceThrowable should call all relevant methods if there is no IOException`() {
        //Given
        val mockThrowableResultProvider: HttpUrlReplacements.ThrowableResultProvider<String> =
            mockk()
        every { mockThrowableResultProvider.get() } returns "result"

        //When
        val result = HttpUrlReplacements.replaceThrowable(
            mockUrlConnection,
            mockThrowableResultProvider,
            true
        )

        //Then
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
        assertEquals("result", result)
    }

    @Test
    fun `reportWithThrowable should call endTracing with unknown response code and exception`() {
        //Given
        val mockException = mockk<IOException>()
        val UNKNOWN_RESPONSE_CODE = -1

        // When
        HttpUrlReplacements.reportWithThrowable(mockUrlConnection, mockException)

        // Then
        verify {
            HttpUrlReplacements.endTracing(
                mockUrlConnection,
                UNKNOWN_RESPONSE_CODE,
                mockException
            )
        }
    }

    @Test
    fun `reportWithResponseCode should call endTracing with response code`() {
        // Given
        val responseCode = 200
        every { mockHttpURLConnection.responseCode } returns responseCode

        // When
        HttpUrlReplacements.reportWithResponseCode(mockHttpURLConnection)

        // Then
        verify { HttpUrlReplacements.endTracing(mockHttpURLConnection, responseCode, null) }
    }

    @Test
    fun `reportWithResponseCode logs exception when IOException occurs`() {
        // Given
        val expectedException = IOException("Test exception")
        every { mockHttpURLConnection.responseCode } throws expectedException

        // When
        HttpUrlReplacements.reportWithResponseCode(mockHttpURLConnection)

        // Then
        verify {
            Log.d(
                "HttpUrlReplacements",
                "An exception was thrown while ending span for connection $mockHttpURLConnection",
                expectedException
            )
        }
    }

    @Test
    fun `replacementForInputStream should call all relevant methods if getInputStream returns a valid stream`() {
        // Given
        every { mockUrlConnection.getInputStream() } returns mockInputStream

        // When
        val result = HttpUrlReplacements.replacementForInputStream(mockUrlConnection)

        // Then
        assertNotNull(result)
        verify { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) }
        verify {
            HttpUrlReplacements.getInstrumentedInputStream(
                mockUrlConnection,
                mockInputStream
            )
        }
    }

    @Test(expected = IOException::class)
    fun `replacementForInputStream should call reportWithThrowable if getInputStream() throws IOException`() {
        // Given
        every { mockUrlConnection.getInputStream() } throws IOException()

        try {
            //When
            HttpUrlReplacements.replacementForInputStream(mockUrlConnection)
        } catch (e: IOException) {
            //Then
            verify { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) }
            verify { HttpUrlReplacements.reportWithThrowable(mockUrlConnection, e) }
            throw e
        }
    }

    @Test
    fun `replacementForInputStream should return null when getInputStream returns null`() {
        // Given
        every { mockUrlConnection.getInputStream() } returns null

        // When
        val result = HttpUrlReplacements.replacementForInputStream(mockUrlConnection)

        // Then
        assertNull(result)
        verify { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) }
    }

    @Test
    fun `replacementForErrorStream should call all relevant methods if getErrorStream returns valid stream`() {
        // Given
        every { mockHttpURLConnection.errorStream } returns mockInputStream

        // When
        val result = HttpUrlReplacements.replacementForErrorStream(mockHttpURLConnection)

        // Then
        assertNotNull(result)
        verify { HttpUrlReplacements.startTracingAtFirstConnection(mockHttpURLConnection) }
        verify {
            HttpUrlReplacements.getInstrumentedInputStream(
                mockHttpURLConnection,
                mockInputStream
            )
        }
    }

    @Test
    fun `replacementForErrorStream should return null stream if getErrorStream returns null`() {
        // Given
        every { mockHttpURLConnection.errorStream } returns null

        // When
        val result = HttpUrlReplacements.replacementForErrorStream(mockHttpURLConnection)

        // Then
        assertNull(result)
        verify { HttpUrlReplacements.startTracingAtFirstConnection(mockHttpURLConnection) }
    }

    @Test
    fun `replacementForDisconnect should call reportWithResponseCode and disconnect for unreported connections`() {
        // Given
        val httpURLConnectionInfo = HttpUrlReplacements.HttpURLConnectionInfo(mockContext)
        HttpUrlReplacements.activeURLConnections[mockHttpURLConnection] = httpURLConnectionInfo
        httpURLConnectionInfo.reported = false

        every { HttpUrlReplacements.reportWithResponseCode(mockHttpURLConnection) } just Runs
        every { mockHttpURLConnection.disconnect() } returns Unit

        // When
        HttpUrlReplacements.replacementForDisconnect(mockHttpURLConnection)

        // Then
        verify { HttpUrlReplacements.reportWithResponseCode(mockHttpURLConnection) }
        verify { mockHttpURLConnection.disconnect() }
    }

    @Test
    fun `replacementForDisconnect should not report if no active connection info`() {
        // Given
        every { mockHttpURLConnection.disconnect() } returns Unit

        // When
        HttpUrlReplacements.replacementForDisconnect(mockHttpURLConnection)

        // Then
        verify(exactly = 0) { HttpUrlReplacements.reportWithResponseCode(mockHttpURLConnection) }
        verify { mockHttpURLConnection.disconnect() }
    }

    @Test
    fun `replacementForDisconnect should not report if connection already reported`() {
        // Given
        val mockInfo = getMockHttpURLConnectionInfo(mockUrlConnection)
        mockInfo.reported = true
        every { mockHttpURLConnection.disconnect() } returns Unit

        // When
        HttpUrlReplacements.replacementForDisconnect(mockHttpURLConnection)

        // Then
        verify(exactly = 0) { HttpUrlReplacements.reportWithResponseCode(mockHttpURLConnection) }
        verify { mockHttpURLConnection.disconnect() }
    }

    @Test
    fun `replacementForConnect should call startTracingAtFirstConnection, connect, and updateLastSeenTime`() {
        // Given
        every { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) } just Runs
        every { HttpUrlReplacements.updateLastSeenTime(mockUrlConnection) } just Runs
        every { mockUrlConnection.connect() } returns Unit

        // When
        HttpUrlReplacements.replacementForConnect(mockUrlConnection)

        // Then
        verify { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) }
        verify { mockUrlConnection.connect() }
        verify { HttpUrlReplacements.updateLastSeenTime(mockUrlConnection) }
    }

    @Test(expected = IOException::class)
    fun `replacementForConnect should throw IOException and call reportWithThrowable`() {
        // Given
        every { mockUrlConnection.connect() } throws IOException()
        every { HttpUrlReplacements.reportWithThrowable(mockUrlConnection, any()) } just Runs

        // When & Then
        try {
            HttpUrlReplacements.replacementForConnect(mockUrlConnection)
        } catch (e: IOException) {
            verify { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) }
            verify { HttpUrlReplacements.reportWithThrowable(mockUrlConnection, e) }
            verify(exactly = 0) { HttpUrlReplacements.updateLastSeenTime(mockUrlConnection) }
            throw e
        }
    }

    @Test
    fun `replacementForContent should call getContent and call functionality within replaceThrowable method`() {
        // Given
        every { HttpUrlReplacements.startTracingAtFirstConnection(any()) } just Runs
        every { HttpUrlReplacements.updateLastSeenTime(any()) } just Runs
        every { HttpUrlReplacements.markHarvestable(any()) } just Runs
        every { mockUrlConnection.content } returns "content"

        // When
        val result = HttpUrlReplacements.replacementForContent(mockUrlConnection)

        // Then
        assert(result == "content")
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test(expected = IOException::class)
    fun `replacementForContent should throw IOException and call reportWithThrowable`() {
        // Given
        every { mockUrlConnection.content } throws IOException()

        // When & Then
        try {
            HttpUrlReplacements.replacementForContent(mockUrlConnection)
        } catch (e: IOException) {
            verify { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) }
            verify { HttpUrlReplacements.reportWithThrowable(mockUrlConnection, any()) }
            throw e
        }
    }

    @Test
    fun `replacementForContent with classes argument should call getContent and call functionality within replaceThrowable method`() {
        // Given
        val expectedContent = "content"
        val classes = arrayOf<Class<*>>()
        every { mockUrlConnection.getContent(classes) } returns expectedContent

        // When
        val result = HttpUrlReplacements.replacementForContent(mockUrlConnection, classes)

        // Then
        assertEquals(expectedContent, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test(expected = IOException::class)
    fun `replacementForContent with classes argument should throw IOException and call reportWithThrowable`() {
        // Given
        val classes = arrayOf<Class<*>>()
        every { mockUrlConnection.getContent(classes) } throws IOException()

        // When & Then
        try {
            HttpUrlReplacements.replacementForContent(mockUrlConnection, classes)
        } catch (e: IOException) {
            verify { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) }
            verify { HttpUrlReplacements.reportWithThrowable(mockUrlConnection, any()) }
            throw e
        }
    }

    @Test
    fun `replacementForContentType should call getContentType and functionality within replace method`() {
        // Given
        val expectedContentType = "text/plain"
        every { mockUrlConnection.contentType } returns expectedContentType

        // When
        val result = HttpUrlReplacements.replacementForContentType(mockUrlConnection)

        // Then
        assertEquals(expectedContentType, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForContentEncoding should call getContentEncoding and functionality within replace method`() {
        // Given
        val expectedContentEncoding = "gzip"
        every { mockUrlConnection.contentEncoding } returns expectedContentEncoding

        // When
        val result = HttpUrlReplacements.replacementForContentEncoding(mockUrlConnection)

        // Then
        assertEquals(expectedContentEncoding, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForContentLength should call getContentLength and functionality within replace method`() {
        // Given
        val expectedLength = 123
        every { mockUrlConnection.contentLength } returns expectedLength

        // When
        val result = HttpUrlReplacements.replacementForContentLength(mockUrlConnection)

        // Then
        assertEquals(expectedLength, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForContentLengthLong should call getContentLengthLong and functionality within replace method`() {
        // Given
        val expectedLengthLong = 123L
        every { mockUrlConnection.contentLengthLong } returns expectedLengthLong

        // When
        val result = HttpUrlReplacements.replacementForContentLengthLong(mockUrlConnection)

        // Then
        assertEquals(expectedLengthLong, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForExpiration should call getExpiration and functionality within replace method`() {
        // Given
        val expectedExpiration = 123456789L
        every { mockUrlConnection.expiration } returns expectedExpiration

        // When
        val result = HttpUrlReplacements.replacementForExpiration(mockUrlConnection)

        // Then
        assertEquals(expectedExpiration, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForDate should call getDate and functionality within replace method`() {
        // Given
        val expectedDate = 987654321L
        every { mockUrlConnection.date } returns expectedDate

        // When
        val result = HttpUrlReplacements.replacementForDate(mockUrlConnection)

        // Then
        assertEquals(expectedDate, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForLastModified should call getLastModified and functionality within replace method`() {
        // Given
        val expectedLastModified = 135792468L
        every { mockUrlConnection.lastModified } returns expectedLastModified

        // When
        val result = HttpUrlReplacements.replacementForLastModified(mockUrlConnection)

        // Then
        assertEquals(expectedLastModified, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForHeaderField should call getHeaderField with field name and functionality within replace method`() {
        // Given
        val headerFieldName = "Content-Type"
        val expectedHeaderValue = "text/html"
        every { mockUrlConnection.getHeaderField(headerFieldName) } returns expectedHeaderValue

        // When
        val result =
            HttpUrlReplacements.replacementForHeaderField(mockUrlConnection, headerFieldName)

        // Then
        assertEquals(expectedHeaderValue, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForHeaderFields should call getHeaderFields and functionality within replace method`() {
        // Given
        val expectedHeaderFields =
            mapOf("Content-Type" to listOf("text/html"), "Content-Length" to listOf("1234"))
        every { mockUrlConnection.headerFields } returns expectedHeaderFields

        // When
        val result = HttpUrlReplacements.replacementForHeaderFields(mockUrlConnection)

        // Then
        assertEquals(expectedHeaderFields, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForHeaderFieldInt should call getHeaderFieldInt with field name and default value and functionality within replace method`() {
        // Given
        val headerFieldName = "Content-Length"
        val defaultValue = 0
        val expectedHeaderValue = 1234
        every {
            mockUrlConnection.getHeaderFieldInt(
                headerFieldName,
                defaultValue
            )
        } returns expectedHeaderValue

        // When
        val result = HttpUrlReplacements.replacementForHeaderFieldInt(
            mockUrlConnection,
            headerFieldName,
            defaultValue
        )

        // Then
        assertEquals(expectedHeaderValue, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForHeaderFieldLong should call getHeaderFieldLong with field name and default value and functionality within replace method`() {
        // Given
        val headerFieldName = "Content-Length"
        val defaultValue = 0L
        val expectedHeaderValue = 1234L
        every {
            mockUrlConnection.getHeaderFieldLong(
                headerFieldName,
                defaultValue
            )
        } returns expectedHeaderValue

        // When
        val result = HttpUrlReplacements.replacementForHeaderFieldLong(
            mockUrlConnection,
            headerFieldName,
            defaultValue
        )

        // Then
        assertEquals(expectedHeaderValue, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForHeaderFieldDate should call getHeaderFieldDate with field name and default value and functionality within replace method`() {
        // Given
        val headerFieldName = "Last-Modified"
        val defaultValue = 0L
        val expectedDateValue = 1609459200000L //timestamp for January 1, 2021
        every {
            mockUrlConnection.getHeaderFieldDate(
                headerFieldName,
                defaultValue
            )
        } returns expectedDateValue

        // When
        val result = HttpUrlReplacements.replacementForHeaderFieldDate(
            mockUrlConnection,
            headerFieldName,
            defaultValue
        )

        // Then
        assertEquals(expectedDateValue, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForHttpHeaderFieldDate should call getHeaderFieldDate with field name and default value and functionality within replace method`() {
        // Given
        val headerFieldName = "Last-Modified"
        val defaultValue = 0L
        val expectedDateValue = 1609459200000L //timestamp for January 1, 2021
        every {
            mockHttpURLConnection.getHeaderFieldDate(
                headerFieldName,
                defaultValue
            )
        } returns expectedDateValue

        // When
        val result = HttpUrlReplacements.replacementForHttpHeaderFieldDate(
            mockHttpURLConnection,
            headerFieldName,
            defaultValue
        )

        // Then
        assertEquals(expectedDateValue, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockHttpURLConnection)
    }

    @Test
    fun `replacementForHeaderFieldKey should call getHeaderFieldKey with index and functionality within replace method`() {
        // Given
        val index = 1
        val expectedKey = "Content-Type"
        every { mockUrlConnection.getHeaderFieldKey(index) } returns expectedKey

        // When
        val result = HttpUrlReplacements.replacementForHeaderFieldKey(mockUrlConnection, index)

        // Then
        assertEquals(expectedKey, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)
    }

    @Test
    fun `replacementForHttpHeaderFieldKey should call getHeaderFieldKey with index and functionality within replace method`() {
        // Given
        val index = 1
        val expectedKey = "Content-Type"
        every { mockHttpURLConnection.getHeaderFieldKey(index) } returns expectedKey

        // When
        val result =
            HttpUrlReplacements.replacementForHttpHeaderFieldKey(mockHttpURLConnection, index)

        // Then
        assertEquals(expectedKey, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockHttpURLConnection)
    }

    @Test
    fun `replacementForHeaderField should call getHeaderField with index and functionality within replace method`() {
        // Given
        val index = 1
        val expectedValue = "text/html"
        every { mockUrlConnection.getHeaderField(index) } returns expectedValue

        // When
        val result = HttpUrlReplacements.replacementForHeaderField(mockUrlConnection, index)

        // Then
        assertEquals(expectedValue, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockUrlConnection)

    }

    @Test
    fun `replacementForHttpHeaderField should call getHeaderField with index and functionality within replace method`() {
        // Given
        val index = 1
        val expectedValue = "text/html"
        every { mockHttpURLConnection.getHeaderField(index) } returns expectedValue

        // When
        val result = HttpUrlReplacements.replacementForHttpHeaderField(mockHttpURLConnection, index)

        // Then
        assertEquals(expectedValue, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockHttpURLConnection)
    }

    @Test
    fun `replacementForResponseCode should call getResponseCode and functionality within replaceThrowable method`() {
        // Given
        val expectedResponseCode = 200
        every { mockHttpURLConnection.responseCode } returns expectedResponseCode

        // When
        val result = HttpUrlReplacements.replacementForResponseCode(mockHttpURLConnection)

        // Then
        assertEquals(expectedResponseCode, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockHttpURLConnection)
    }

    @Test(expected = IOException::class)
    fun `replacementForResponseCode should throw IOException and call reportWithThrowable`() {
        // Given
        every { mockHttpURLConnection.responseCode } throws IOException()

        // When & Then
        try {
            HttpUrlReplacements.replacementForResponseCode(mockHttpURLConnection)
        } catch (e: IOException) {
            verify { HttpUrlReplacements.startTracingAtFirstConnection(mockHttpURLConnection) }
            verify { HttpUrlReplacements.reportWithThrowable(mockHttpURLConnection, any()) }
            throw e
        }
    }

    @Test
    fun `replacementForResponseMessage should call getResponseMessage and functionality within replaceThrowable method`() {
        // Given
        val expectedResponseMessage = "OK"
        every { mockHttpURLConnection.responseMessage } returns expectedResponseMessage

        // When
        val result = HttpUrlReplacements.replacementForResponseMessage(mockHttpURLConnection)

        // Then
        assertEquals(expectedResponseMessage, result)
        verifyReplaceAndReplaceThrowableFunctionalityIsCalled(mockHttpURLConnection)
    }

    @Test(expected = IOException::class)
    fun `replacementForResponseMessage should throw IOException and call reportWithThrowable`() {
        // Given
        every { mockHttpURLConnection.responseMessage } throws IOException()

        // When & Then
        try {
            HttpUrlReplacements.replacementForResponseMessage(mockHttpURLConnection)
        } catch (e: IOException) {
            verify { HttpUrlReplacements.startTracingAtFirstConnection(mockHttpURLConnection) }
            verify { HttpUrlReplacements.reportWithThrowable(mockHttpURLConnection, any()) }
            throw e
        }
    }

    @Test
    fun `replacementForOutputStream should call getOutputStream and startTracingAtFirstConnection and updateLastSeenTime`() {
        // Given
        val outputStream = mockk<OutputStream>()
        every { mockUrlConnection.outputStream } returns outputStream

        // When
        val result = HttpUrlReplacements.replacementForOutputStream(mockUrlConnection)

        // Then
        assertEquals(outputStream, result)
        verify { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) }
        verify { HttpUrlReplacements.updateLastSeenTime(mockUrlConnection) }
    }

    @Test(expected = IOException::class)
    fun `replacementForOutputStream should throw IOException and call reportWithThrowable`() {
        // Given
        every { mockUrlConnection.outputStream } throws IOException()

        // When & Then
        try {
            HttpUrlReplacements.replacementForOutputStream(mockUrlConnection)
        } catch (e: IOException) {
            verify { HttpUrlReplacements.startTracingAtFirstConnection(mockUrlConnection) }
            verify { HttpUrlReplacements.reportWithThrowable(mockUrlConnection, any()) }
            throw e
        }
    }

    @Test
    fun `read single byte should return correct result and call markHarvestable when not end of stream`() {
        //Given
        val expectedValue = 42
        every { mockInputStream.read() } returns expectedValue

        //When
        val instrumentedInputStream =
            HttpUrlReplacements.getInstrumentedInputStream(mockHttpURLConnection, mockInputStream)
        val result = instrumentedInputStream.read()

        //Then
        assertEquals(expectedValue, result)
        verify(exactly = 1) { mockInputStream.read() }
        verify(exactly = 1) { HttpUrlReplacements.markHarvestable(mockHttpURLConnection) }
    }

    @Test
    fun `read single byte should report when end of stream is reached and not call markHarvestable`() {
        //Given
        val expectedValue = -1
        every { mockInputStream.read() } returns expectedValue
        every { mockHttpURLConnection.responseCode } returns 200

        //When
        val instrumentedInputStream =
            HttpUrlReplacements.getInstrumentedInputStream(mockHttpURLConnection, mockInputStream)
        val result = instrumentedInputStream.read()

        //Then
        assertEquals(expectedValue, result)
        verify(exactly = 1) { mockInputStream.read() }
        verify(exactly = 1) { mockHttpURLConnection.responseCode }
        verify(exactly = 0) { HttpUrlReplacements.markHarvestable(mockHttpURLConnection) }
    }

    @Test(expected = IOException::class)
    fun `read single byte should throw IOException and report`() {
        //Given
        every { mockInputStream.read() } throws IOException()

        //When
        val instrumentedInputStream =
            HttpUrlReplacements.getInstrumentedInputStream(mockHttpURLConnection, mockInputStream)

        //Then
        try {
            instrumentedInputStream.read()
        } finally {
            verify(exactly = 1) { mockInputStream.read() }
            verify {
                HttpUrlReplacements.reportWithThrowable(
                    mockHttpURLConnection,
                    any<IOException>()
                )
            }
        }
    }

    @Test
    fun `read byte array should return correct result and call markHarvestable when not end of stream`() {
        //Given
        val buffer = ByteArray(10)
        val expectedValue = 10
        every { mockInputStream.read(buffer) } returns expectedValue

        //When
        val instrumentedInputStream =
            HttpUrlReplacements.getInstrumentedInputStream(mockHttpURLConnection, mockInputStream)
        val result = instrumentedInputStream.read(buffer)

        //Then
        assertEquals(expectedValue, result)
        verify(exactly = 1) { mockInputStream.read(buffer) }
        verify(exactly = 1) { HttpUrlReplacements.markHarvestable(mockHttpURLConnection) }
    }

    @Test
    fun `read byte array should report connection when end of stream is reached and not call markHarvestable`() {
        //Given
        val buffer = ByteArray(10)
        val expectedValue = -1
        every { mockInputStream.read(buffer) } returns expectedValue
        every { mockHttpURLConnection.responseCode } returns 200

        //When
        val instrumentedInputStream =
            HttpUrlReplacements.getInstrumentedInputStream(mockHttpURLConnection, mockInputStream)
        val result = instrumentedInputStream.read(buffer)

        //Then
        assertEquals(expectedValue, result)
        verify(exactly = 1) { mockInputStream.read(buffer) }
        verify(exactly = 1) { mockHttpURLConnection.responseCode }
        verify(exactly = 0) { HttpUrlReplacements.markHarvestable(mockHttpURLConnection) }
    }

    @Test(expected = IOException::class)
    fun `read byte array should throw IOException and report`() {
        //Given
        val buffer = ByteArray(10)
        every { mockInputStream.read(buffer) } throws IOException()

        //When
        val instrumentedInputStream =
            HttpUrlReplacements.getInstrumentedInputStream(mockHttpURLConnection, mockInputStream)

        //Then
        try {
            instrumentedInputStream.read(buffer)
        } finally {
            verify(exactly = 1) { mockInputStream.read(buffer) }
            verify {
                HttpUrlReplacements.reportWithThrowable(
                    mockHttpURLConnection,
                    any<IOException>()
                )
            }
        }
    }

    @Test
    fun `read byte array from off to len should return correct result and call markHarvestable when not end of stream`() {
        //Given
        val buffer = ByteArray(15)
        val bytesRead = 10
        every { mockInputStream.read(buffer, 0, bytesRead) } returns bytesRead

        //When
        val instrumentedInputStream =
            HttpUrlReplacements.getInstrumentedInputStream(mockHttpURLConnection, mockInputStream)
        val result = instrumentedInputStream.read(buffer, 0, bytesRead)

        //Then
        assertEquals(bytesRead, result)
        verify(exactly = 1) { mockInputStream.read(buffer, 0, bytesRead) }
        verify(exactly = 1) { HttpUrlReplacements.markHarvestable(mockHttpURLConnection) }
    }

    @Test
    fun `read byte array from off to len should report connection when end of stream is reached and not call markHarvestable`() {
        //Given
        val buffer = ByteArray(10)
        val expectedValue = -1
        every { mockInputStream.read(buffer, 0, buffer.size) } returns expectedValue
        every { mockHttpURLConnection.responseCode } returns 200

        //When
        val instrumentedInputStream =
            HttpUrlReplacements.getInstrumentedInputStream(mockHttpURLConnection, mockInputStream)
        val result = instrumentedInputStream.read(buffer, 0, buffer.size)

        //Then
        assertEquals(expectedValue, result)
        verify(exactly = 1) { mockInputStream.read(buffer, 0, buffer.size) }
        verify(exactly = 1) { mockHttpURLConnection.responseCode }
        verify(exactly = 0) { HttpUrlReplacements.markHarvestable(mockHttpURLConnection) }
    }

    @Test(expected = IOException::class)
    fun `read byte array from off to len should throw IOException and report`() {
        //Given
        val buffer = ByteArray(10)
        every { mockInputStream.read(buffer, 0, buffer.size) } throws IOException()

        //When
        val instrumentedInputStream =
            HttpUrlReplacements.getInstrumentedInputStream(mockHttpURLConnection, mockInputStream)

        //Then
        try {
            instrumentedInputStream.read(buffer, 0, buffer.size)
        } finally {
            verify(exactly = 1) { mockInputStream.read(buffer, 0, buffer.size) }
            verify {
                HttpUrlReplacements.reportWithThrowable(
                    mockHttpURLConnection,
                    any<IOException>()
                )
            }
        }
    }

    @Test
    fun `close should report with response code and close the input stream`() {
        //Given
        every { mockHttpURLConnection.responseCode } returns 200
        every { mockInputStream.close() } returns Unit

        //When
        val instrumentedInputStream =
            HttpUrlReplacements.getInstrumentedInputStream(mockHttpURLConnection, mockInputStream)
        instrumentedInputStream.close()

        //Then
        verify(exactly = 1) { mockInputStream.close() }
        verify { HttpUrlReplacements.reportWithResponseCode(mockHttpURLConnection) }
    }

    @Test
    fun `reportIdleConnectionsOlderThan should report harvestable, not reported connections older than the interval`() {
        // Given
        val mockTimeNow = 123456789L //same as mock SystemClock.uptimeMillis() value
        val mockTimeInterval = 10000L
        val mockOlderTime = mockTimeNow - mockTimeInterval - 1

        val mockInfo = getMockHttpURLConnectionInfo(mockHttpURLConnection)
        mockInfo.harvestable = true
        mockInfo.reported = false
        mockInfo.lastSeenTime = mockOlderTime

        every { HttpUrlReplacements.reportWithResponseCode(mockHttpURLConnection) } returns Unit

        // When
        HttpUrlReplacements.reportIdleConnectionsOlderThan(mockTimeInterval)

        // Then
        verify { HttpUrlReplacements.reportWithResponseCode(mockHttpURLConnection) }
    }

    @Test
    fun `reportIdleConnectionsOlderThan should not report harvestable, not reported connections newer than the interval`() {
        // Given
        val timeNow = 123456789L //same as mock SystemClock.uptimeMillis() value
        val mockTimeInterval = 10000L
        val mockNewerTime = timeNow - mockTimeInterval + 1L

        val mockInfo = getMockHttpURLConnectionInfo(mockHttpURLConnection)
        mockInfo.harvestable = true
        mockInfo.reported = false
        mockInfo.lastSeenTime = mockNewerTime

        // When
        HttpUrlReplacements.reportIdleConnectionsOlderThan(mockTimeInterval)

        // Then
        verify(exactly = 0) { HttpUrlReplacements.reportWithResponseCode(any()) }
    }

    @Test
    fun `reportIdleConnectionsOlderThan should not report already reported connections`() {
        // Given
        val mockInfo = getMockHttpURLConnectionInfo(mockHttpURLConnection)
        mockInfo.reported = true
        val mockTimeInterval = 10000L

        // When
        HttpUrlReplacements.reportIdleConnectionsOlderThan(mockTimeInterval)

        // Then
        verify(exactly = 0) { HttpUrlReplacements.reportWithResponseCode(any()) }
    }

    @Test
    fun `reportIdleConnectionsOlderThan should not report non harvestable connections`() {
        // Given
        val mockInfo = getMockHttpURLConnectionInfo(mockHttpURLConnection)
        mockInfo.harvestable = false
        val mockTimeInterval = 10000L

        // When
        HttpUrlReplacements.reportIdleConnectionsOlderThan(mockTimeInterval)

        // Then
        verify(exactly = 0) { HttpUrlReplacements.reportWithResponseCode(any()) }
    }

    private fun verifyReplaceAndReplaceThrowableFunctionalityIsCalled(connection: URLConnection) {
        verify { HttpUrlReplacements.startTracingAtFirstConnection(connection) }
        verify { HttpUrlReplacements.updateLastSeenTime(connection) }
        verify { HttpUrlReplacements.markHarvestable(connection) }
    }

    private fun getMockHttpURLConnectionInfo(connection: URLConnection): HttpUrlReplacements.HttpURLConnectionInfo {
        val httpURLConnectionInfo = HttpUrlReplacements.HttpURLConnectionInfo(mockContext)
        HttpUrlReplacements.activeURLConnections[connection] = httpURLConnectionInfo
        return httpURLConnectionInfo
    }
}
