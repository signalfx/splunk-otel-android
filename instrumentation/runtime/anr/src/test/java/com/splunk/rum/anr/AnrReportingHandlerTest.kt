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

package com.splunk.rum.anr

import android.app.Application
import android.content.Context
import android.os.Looper
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import android.view.Choreographer
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import com.cisco.mrum.common.otel.api.OpenTelemetry
import com.splunk.rum.anr.AnrReportingHandler
import io.mockk.Runs
import io.mockk.mockkObject
import io.mockk.just
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider

class AnrReportingHandlerTest {

    private class TestSetup {
        val anrHandler: AnrReportingHandler
        val mockLooper: Looper
        val mockThread: Thread
        val mockApplication: Application
        val mockContext: Context
        val mockChoreographer: Choreographer
        val mockScheduler: ScheduledThreadPoolExecutor

        init {
            mockLooper = mockk()
            mockThread = mockk()
            mockkStatic(Looper::class)
            every { Looper.getMainLooper() } returns mockLooper

            mockApplication = mockk {
                every { registerActivityLifecycleCallbacks(any()) } returns Unit
                every { unregisterActivityLifecycleCallbacks(any()) } returns Unit
            }
            mockContext = mockk {
                every { applicationContext } returns mockApplication
            }

            mockChoreographer = mockk(relaxed = true)
            mockScheduler = mockk(relaxed = true)
            mockkStatic(Choreographer::class)
            every { Choreographer.getInstance() } returns mockChoreographer

            every { mockLooper.thread } returns Thread.currentThread()

            anrHandler = AnrReportingHandler(context = mockContext).apply {
                looper = mockLooper
                scheduler = mockScheduler
            }
            anrHandler.looper = mockLooper
        }
    }

    private fun test(block: TestSetup.() -> Unit) {
        val testSetup = TestSetup()
        block.invoke(testSetup)
    }

    @Test
    fun `when obtain main stack trace is called`() = test {
        givenMockStackTraceThatIsReturnedByMockLooper()
        val stackTraceString = whenObtainMainThreadStackTraceIsCalled()
        thenStackTraceStringContainsElementsFromMockStackTrace(stackTraceString)
    }

    @Test
    fun `when anr reporting handler is registered`() = test {
        givenAnrHandlerIsNotRegistered()
        whenRegisterIsCalled(5L)
        thenAnrHandlerIsRegistered()
        thenAnrHandlerFutureIsNotNull()
        thenAnrHandlerWatchdogIsNotNull()
        thenSchedulerIsAppropriatelyScheduled()
    }

    @Test
    fun `when anr reporting handler is unregistered`() = test {
        givenAnrHandlerIsNotRegistered()
        whenRegisterIsCalled(5L)
        thenAnrHandlerIsRegistered()
        whenUnregisterIsCalled()
        thenAnrHandlerIsNotRegistered()
        thenAnrHandlerFutureIsNull()
    }

    @Test
    fun `when app state changes`() = test {
        val listener = givenTheAppStateObserverListener()

        listener.apply {
            whenAppStarted()
            thenCurrentAppStateIs(AnrReportingHandler.CurrentAppState.CREATED)

            whenAppBackgrounded()
            thenCurrentAppStateIs(AnrReportingHandler.CurrentAppState.BACKGROUND)

            whenAppForegrounded()
            thenCurrentAppStateIs(AnrReportingHandler.CurrentAppState.FOREGROUND)
        }
    }

    @Test
    fun `when anr log record body is generated`() = test {
        givenAppStateObserverOnAppStarted()
        val result = whenGenerateANRLogRecordBodyIsCalled()
        thenANRLogRecordBodyContainsExpectedElements(result)
    }

    @Test
    fun `when log record is built`() = test {
        val (mockLogRecordBuilder, mockSdkLoggerProvider) = givenOpentelemetryInstanceAndLogRecordBuilder()
        whenANRHandlerIsRegisteredAndOnAppNotRespondingIsTriggered()
        thenLogRecordBuilderMethodsAreCalledWithCorrectParameters(mockLogRecordBuilder, mockSdkLoggerProvider)
    }

    private fun TestSetup.givenMockStackTraceThatIsReturnedByMockLooper() {
        val mockStackTrace = arrayOf(
            StackTraceElement("MyClass", "myMethod", "MyClass.kt", 123),
            StackTraceElement("MyClass", "myOtherMethod", "MyClass.kt", 456)
        )
        every { Looper.getMainLooper() } returns mockLooper
        every { mockLooper.thread } returns mockThread
        every { mockThread.stackTrace } returns mockStackTrace
    }

    private fun TestSetup.whenObtainMainThreadStackTraceIsCalled(): String {
        return anrHandler.obtainMainThreadStackTrace()
    }

    private fun TestSetup.thenStackTraceStringContainsElementsFromMockStackTrace(stackTraceString: String) {
        assertTrue(stackTraceString.contains("MyClass.myMethod(MyClass.kt:123)"))
        assertTrue(stackTraceString.contains("MyClass.myOtherMethod(MyClass.kt:456)"))
    }

    private fun TestSetup.givenAnrHandlerIsNotRegistered() {
        assertEquals(anrHandler.isRegistered, false)
    }

    private fun TestSetup.thenAnrHandlerIsRegistered() {
        assertEquals(anrHandler.isRegistered, true)
    }

    private fun TestSetup.thenAnrHandlerIsNotRegistered() {
        assertEquals(anrHandler.isRegistered, false)
    }

    private fun TestSetup.thenAnrHandlerFutureIsNotNull() {
        assertNotNull(anrHandler.future)
    }

    private fun TestSetup.thenSchedulerIsAppropriatelyScheduled() {
        verify { anrHandler.scheduler.scheduleAtFixedRate(anrHandler.anrWatchDog, eq(1L), eq(1L), eq(TimeUnit.SECONDS)) }
    }

    private fun TestSetup.thenAnrHandlerWatchdogIsNotNull() {
        assertNotNull(anrHandler.anrWatchDog)
    }

    private fun TestSetup.thenAnrHandlerFutureIsNull() {
        assertNull(anrHandler.future)
    }

    private fun TestSetup.whenRegisterIsCalled(thresholdSeconds: Long) {
        anrHandler.register(thresholdSeconds)
    }

    private fun TestSetup.whenUnregisterIsCalled() {
        anrHandler.unregister()
    }

    private fun TestSetup.givenTheAppStateObserverListener(): AnrReportingHandler.AppStateObserverListener {
        return anrHandler.AppStateObserverListener()
    }

    private fun AnrReportingHandler.AppStateObserverListener.whenAppStarted() {
        onAppStarted()
    }

    private fun AnrReportingHandler.AppStateObserverListener.whenAppBackgrounded() {
        onAppBackgrounded()
    }

    private fun AnrReportingHandler.AppStateObserverListener.whenAppForegrounded() {
        onAppForegrounded()
    }

    private fun TestSetup.thenCurrentAppStateIs(expectedState: AnrReportingHandler.CurrentAppState) {
        assertEquals(expectedState, anrHandler.currentAppState)
    }

    private fun TestSetup.givenAppStateObserverOnAppStarted() {
        anrHandler.AppStateObserverListener().onAppStarted()
    }

    private fun TestSetup.whenGenerateANRLogRecordBodyIsCalled(): String {
        val mockThread = mockk<Thread>()
        every { mockThread.id } returns 1
        every { mockThread.name } returns "TestThread"
        return anrHandler.generateANRLogRecordBody(mockThread)
    }

    private fun TestSetup.thenANRLogRecordBodyContainsExpectedElements(result: String) {
        assertTrue(result.contains("\"exception.stacktrace\":\"java.base\\/java.lang.Thread.getStackTrace"));
        assertTrue(result.contains("\"thread.id\":1"));
        assertTrue(result.contains("\"thread.name\":\"TestThread\""));
        assertTrue(result.contains("\"android.state\":\"created\""));
    }

    private fun TestSetup.givenOpentelemetryInstanceAndLogRecordBuilder(): Pair<LogRecordBuilder, SdkLoggerProvider> {
        val mockOpenTelemetrySdk = mockk<OpenTelemetrySdk>()
        val mockSdkLoggerProvider = mockk<SdkLoggerProvider>()
        val mockLogRecordBuilder = mockk<LogRecordBuilder>().apply {
            every { setTimestamp(any(), TimeUnit.MILLISECONDS) } returns this
            every { setObservedTimestamp(any(), TimeUnit.MILLISECONDS) } returns this
            every { setBody(any()) } returns this
            every { setAllAttributes(any()) } returns this
            every { setSeverity(Severity.WARN) } returns this
            every { emit() } just Runs
        }
        mockkObject(OpenTelemetry)
        every { OpenTelemetry.instance } returns mockOpenTelemetrySdk
        every { mockOpenTelemetrySdk.sdkLoggerProvider } returns mockSdkLoggerProvider
        every {
            mockSdkLoggerProvider
                .loggerBuilder("ANRInstrumentationScope")
                .build()
                .logRecordBuilder()
        } returns mockLogRecordBuilder

        return Pair(mockLogRecordBuilder, mockSdkLoggerProvider)
    }

    private fun TestSetup.whenANRHandlerIsRegisteredAndOnAppNotRespondingIsTriggered() {
        anrHandler.register(5L)
        anrHandler.anrWatchDog.anrListener.onAppNotResponding()
    }

    private fun TestSetup.thenLogRecordBuilderMethodsAreCalledWithCorrectParameters(
        mockLogRecordBuilder: LogRecordBuilder,
        mockSdkLoggerProvider: SdkLoggerProvider
    ) {
        verify {
            mockSdkLoggerProvider
                .loggerBuilder("ANRInstrumentationScope")
                .build()
                .logRecordBuilder()
        }
        verify {
            mockLogRecordBuilder.setTimestamp(any(), TimeUnit.MILLISECONDS)
            mockLogRecordBuilder.setObservedTimestamp(any(), TimeUnit.MILLISECONDS)
            mockLogRecordBuilder.setBody(match { it.contains("exception.stacktrace") })
            mockLogRecordBuilder.setAllAttributes(match { it.get(AttributeKey.stringKey("event.name")) == "device.app.anr" })
            mockLogRecordBuilder.setSeverity(Severity.WARN)
            mockLogRecordBuilder.emit()
        }
    }
}
