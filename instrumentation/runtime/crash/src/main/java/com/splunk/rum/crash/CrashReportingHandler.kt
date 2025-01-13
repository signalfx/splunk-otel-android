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
import android.os.HandlerThread
import android.util.Log
import com.cisco.android.common.logger.Logger
import com.cisco.android.common.utils.AppStateObserver
import com.splunk.sdk.common.otel.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit

class CrashReportingHandler(
    context: Context,
) {
    companion object {
        private const val TAG = "CrashReportingHandler"
        private const val CRASH_EVENT_NAME = "device.app.crash"
        private const val CHECK_INTERVAL: Long = 10000 // Check every 10 seconds
    }

    private val appStateObserver = AppStateObserver()

    enum class CurrentAppState(private val stringValue: String) {
        FOREGROUND("foreground"),
        BACKGROUND("background"),
        CREATED("created");

        override fun toString() = stringValue
    }

    internal lateinit var currentAppState: CurrentAppState
    private var isRegistered: Boolean = false
    private var originalUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private val ourExceptionHandler: Thread.UncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
        uncaughtException(thread, throwable)
    }
    private lateinit var checkExceptionHandlerTask: Runnable
    private var handler: Handler

    init {
        appStateObserver.listener = AppStateObserverListener()
        appStateObserver.attach(context.applicationContext as Application)
        handler = HandlerProvider.getHandler()
        initializeCheckExceptionHandler()
    }

    private fun initializeCheckExceptionHandler() {
        checkExceptionHandlerTask = Runnable { checkIfExceptionHandlerOverridden(handler!!) }
    }

    fun register() {
        if (!isRegistered) {
            Logger.d(TAG, "CrashReportingHandler register() called")
            originalUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(ourExceptionHandler)

            handler?.postDelayed(checkExceptionHandlerTask, CHECK_INTERVAL)
            isRegistered = true
        }
    }

    fun unregister() {
        if (isRegistered) {
            Logger.d(TAG, "CrashReportingHandler unregister() called")
            originalUncaughtExceptionHandler?.let {
                Thread.setDefaultUncaughtExceptionHandler(it)
            }
            handler?.removeCallbacks(checkExceptionHandlerTask)
            isRegistered = false
        }
    }

    private fun checkIfExceptionHandlerOverridden(handler: Handler) {
        val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (currentHandler !== ourExceptionHandler) {
            // Another SDK or component has overridden the handler
            Logger.d(TAG, "Cisco CrashReportingHandler may be conflicting with another third party library, crash log events may not be generated as the default exception handler has been overridden")
            // Stop task
            handler.removeCallbacks(checkExceptionHandlerTask)
        } else {
            // Repost the task to run again after check interval duration
            handler.postDelayed(checkExceptionHandlerTask, CHECK_INTERVAL)
        }
    }

    private fun uncaughtException(thread: Thread, throwable: Throwable) {
        val timestamp = System.currentTimeMillis() // Current timestamp immediately when crash happens
        Logger.w(TAG, Log.getStackTraceString(throwable))

        val inst = OpenTelemetry.instance
        val loggerProvider = inst?.sdkLoggerProvider

        if (loggerProvider != null) {
            loggerProvider
                .loggerBuilder("CrashInstrumentationScope")
                .build()
                .logRecordBuilder()
                .setTimestamp(timestamp, TimeUnit.MILLISECONDS)
                .setObservedTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setBody(generateCrashLogRecordBody(thread, throwable))
                .setAllAttributes(generateCrashLogRecordAttributes())
                // .setSeverity(Severity.DEBUG) // currently undecided whether severity level will be used in crash events
                .emit()
        }

        // TODO This has to be fixed, it is just temp solution.
        loggerProvider?.forceFlush()?.join(2, TimeUnit.SECONDS)

        originalUncaughtExceptionHandler?.uncaughtException(thread, throwable)
    }

    internal fun generateCrashLogRecordBody(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stacktrace = sw.toString()

        val json = JSONObject()
            .put("exception.stacktrace", stacktrace)
            .put("exception.message", throwable.message ?: "")
            .put("thread.id", thread.id)
            .put("thread.name", thread.name)

        if (this::currentAppState.isInitialized) {
            json.put("android.state", currentAppState)
        }
        return json.toString()
    }

    private fun generateCrashLogRecordAttributes(): Attributes {
        return Attributes.of(
            AttributeKey.stringKey("event.name"), CRASH_EVENT_NAME,
        )
    }

    // TODO Rewrite this modules such that we aren't duplicating AppStateObserver
    internal inner class AppStateObserverListener : AppStateObserver.Listener {
        override fun onAppBackgrounded() {
            currentAppState = CurrentAppState.BACKGROUND
        }

        override fun onAppForegrounded() {
            currentAppState = CurrentAppState.FOREGROUND
        }

        override fun onAppStarted() {
            currentAppState = CurrentAppState.CREATED
        }
    }

    internal object HandlerProvider {
        private var providedHandler: Handler? = null
        fun getHandler(): Handler {
            // Handler object would be non null with a mock handler returned by mock handler provider when called from unit tests
            // Otherwise when crashReportingHandler is initialized from host app, handler provider is null and handler gets created here
            if (providedHandler == null) {
                val checkExceptionHandlerThread = HandlerThread("CheckExceptionHandlerThread")
                checkExceptionHandlerThread.start()
                providedHandler = Handler(checkExceptionHandlerThread.looper)
            }
            return providedHandler!!
        }
    }
}
