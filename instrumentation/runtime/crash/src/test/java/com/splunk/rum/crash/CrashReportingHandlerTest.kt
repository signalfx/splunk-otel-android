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

package com.splunk.rum.crash

import android.app.Application
import android.content.Context
import android.os.Handler
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import android.view.Choreographer
import com.splunk.rum.crash.CrashReportingHandler
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.just
import io.mockk.mockkObject

class CrashReportingHandlerTest {
    private class TestSetup {
        val mockApplication: Application = mockk {
            every { registerActivityLifecycleCallbacks(any()) } returns Unit
            every { unregisterActivityLifecycleCallbacks(any()) } returns Unit
        }
        val mockContext: Context = mockk {
            every { applicationContext } returns mockApplication
        }
        val mockChoreographer: Choreographer = mockk(relaxed = true)
        val mockHandler: Handler = mockk()

        init {
            mockkStatic(Choreographer::class)
            every { Choreographer.getInstance() } returns mockChoreographer

            mockkObject(CrashReportingHandler.HandlerProvider)
            every { CrashReportingHandler.HandlerProvider.getHandler() } returns mockHandler

            every { mockHandler.postDelayed(any(), any()) } returns true
            every { mockHandler.removeCallbacks(any()) } just Runs
        }
        val crashHandler by lazy {
            CrashReportingHandler(context = mockContext)
        }
    }

    private fun test(block: TestSetup.() -> Unit) {
        val testSetup = TestSetup()
        block.invoke(testSetup)
    }

    @Test
    fun `when custom crash handler is registered`() = test {
        val defaultExceptionHandlerBefore = givenThereIsADefaultAndroidExceptionHandler()

        whenCustomExceptionHandlerRegistered()

        thenCurrentExceptionHandlerIsNotOriginalHandler(defaultExceptionHandlerBefore,
            Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun `when custom crash handler is unregistered`() = test {
        val defaultExceptionHandlerBefore = givenThereIsADefaultAndroidExceptionHandler()

        whenCustomExceptionHandlerRegistered()
        whenCustomExceptionHandlerUnregistered()

        thenCurrentExceptionHandlerIsOriginalHandler(defaultExceptionHandlerBefore,
            Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun `when crash log record body is generated`() = test {
        // Given a thread and exception with certain properties
        val mockThread = givenAMockThread()
        val testException = givenATestException()

        // When the crash log record body is generated
        val result = whenCrashLogRecordBodyGenerated(mockThread, testException)

        // Then the result should contain specific elements
        thenResultContainsExpectedElements(result)
    }

    @Test
    fun `when app state changes`() = test {
        val listener = givenTheAppStateObserverListener()

        listener.apply {
            whenAppStarted()
            thenCurrentAppStateIs(CrashReportingHandler.CurrentAppState.CREATED)

            whenAppBackgrounded()
            thenCurrentAppStateIs(CrashReportingHandler.CurrentAppState.BACKGROUND)

            whenAppForegrounded()
            thenCurrentAppStateIs(CrashReportingHandler.CurrentAppState.FOREGROUND)
        }
    }

    private fun TestSetup.givenTheAppStateObserverListener(): CrashReportingHandler.AppStateObserverListener {
        return crashHandler.AppStateObserverListener()
    }

    private fun givenThereIsADefaultAndroidExceptionHandler(): Thread.UncaughtExceptionHandler {
        return registerMockAndroidDefaultHandler()
    }

    private fun givenAMockThread(): Thread {
        val mockThread = mockk<Thread>()
        every { mockThread.id } returns 1
        every { mockThread.name } returns "TestThread"
        return mockThread
    }

    private fun givenATestException(): RuntimeException {
        return RuntimeException("Test Exception")
    }

    private fun CrashReportingHandler.AppStateObserverListener.whenAppStarted() {
        onAppStarted()
    }

    private fun CrashReportingHandler.AppStateObserverListener.whenAppBackgrounded() {
        onAppBackgrounded()
    }

    private fun CrashReportingHandler.AppStateObserverListener.whenAppForegrounded() {
        onAppForegrounded()
    }

    private fun TestSetup.whenCrashLogRecordBodyGenerated(mockThread: Thread, testException: RuntimeException): String {
        return crashHandler.generateCrashLogRecordBody(mockThread, testException)
    }

    private fun TestSetup.whenCustomExceptionHandlerRegistered() {
        crashHandler.register()
    }

    private fun TestSetup.whenCustomExceptionHandlerUnregistered() {
        crashHandler.unregister()
    }

    private fun thenResultContainsExpectedElements(result: String) {
        assertTrue(result.contains("exception.stacktrace"))
        assertTrue(result.contains("exception.message"))
        assertTrue(result.contains("thread.id"))
        assertTrue(result.contains("thread.name"))
    }

    private fun thenCurrentExceptionHandlerIsOriginalHandler(
        defaultExceptionHandlerBefore: Thread.UncaughtExceptionHandler?,
        defaultExceptionHandlerAfter: Thread.UncaughtExceptionHandler?
    ) {
        assertEquals(defaultExceptionHandlerAfter, defaultExceptionHandlerBefore)
    }

    private fun thenCurrentExceptionHandlerIsNotOriginalHandler(
        defaultExceptionHandlerBefore: Thread.UncaughtExceptionHandler?,
        defaultExceptionHandlerAfter: Thread.UncaughtExceptionHandler?
    ) {
        assertNotEquals(defaultExceptionHandlerAfter, defaultExceptionHandlerBefore)
    }

    private fun TestSetup.thenCurrentAppStateIs(expectedState: CrashReportingHandler.CurrentAppState) {
        assertEquals(expectedState, crashHandler.currentAppState)
    }

    private fun registerMockAndroidDefaultHandler(): Thread.UncaughtExceptionHandler {
        val testHandler = MockDefaultAndroidUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(testHandler)
        return testHandler
    }
}

// In this test environment, Thread.getDefaultUncaughtExceptionHandler() returns null because
// the Android runtime environment is not fully initialized so we have to mock the default handler
class MockDefaultAndroidUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
    }
}
