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

package com.cisco.android.rum.anr

import android.os.Handler
import android.os.Looper
import com.smartlook.sdk.log.LogAspect
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import com.cisco.mrum.common.otel.api.OpenTelemetry
import com.smartlook.sdk.common.logger.Logger
import android.app.Application
import android.content.Context
import com.smartlook.sdk.common.utils.AppStateObserver
import io.opentelemetry.api.logs.Severity

class AnrReportingHandler(
    context: Context
) {
    companion object {
        private const val TAG = "AnrReportingHandler"
        private const val ANR_EVENT_NAME = "device.app.anr"
    }

    private val appStateObserver = AppStateObserver()
    var looper: Looper? = null // only set to mock looper in case of unit tests

    enum class CurrentAppState(private val stringValue: String) {
        FOREGROUND("foreground"),
        BACKGROUND("background"),
        CREATED("created");

        override fun toString() = stringValue
    }

    internal lateinit var currentAppState: CurrentAppState
    internal lateinit var anrWatchDog: ANRWatchDog
    internal var isRegistered: Boolean = false
    internal var scheduler = Executors.newScheduledThreadPool(1)
    internal var future: ScheduledFuture<*>? = null

    init {
        appStateObserver.listener = AppStateObserverListener()
        appStateObserver.attach(context.applicationContext as Application)
    }

    fun register(thresholdSeconds: Long) {
        if (!isRegistered) {
            anrWatchDog = ANRWatchDog(Handler(Looper.getMainLooper()), object : ANRWatchDog.ANRListener {
                override fun onAppNotResponding() {
                    val timestamp = System.currentTimeMillis() // Current timestamp immediately when anr happens
                    Logger.privateD(LogAspect.CRASH_TRACKING, TAG, { "onAppNotResponding() called" })

                    val inst = OpenTelemetry.instance
                    val loggerProvider = inst?.sdkLoggerProvider

                    val mainLooper = Looper.getMainLooper() // Assuming the ANR is occurring in the main ui thread

                    if (loggerProvider != null) {
                        loggerProvider
                            .loggerBuilder("ANRInstrumentationScope")
                            .build()
                            .logRecordBuilder()
                            .setTimestamp(timestamp, TimeUnit.MILLISECONDS)
                            .setObservedTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            .setBody(generateANRLogRecordBody(mainLooper.thread))
                            .setAllAttributes(generateANRLogRecordAttributes())
                            .setSeverity(Severity.WARN)
                            .emit()
                    }
                }
            }, thresholdSeconds, true)

            if (future == null) {
                future = scheduler.scheduleAtFixedRate(anrWatchDog, 1, 1, TimeUnit.SECONDS)
            }
            isRegistered = true
        }
    }

    fun unregister() {
        if (isRegistered) {
            future?.cancel(true)
            future = null
            isRegistered = false
        }
    }

    internal fun obtainMainThreadStackTrace(): String {
        // currentLooper will be mock looper for unit tests, and always Looper.getMainLooper() otherwise
        val currentLooper = looper ?: Looper.getMainLooper()

        return StringBuilder().apply {
            currentLooper.thread.stackTrace.forEach {
                append(it.toString())
                append("\n")
            }
        }.toString()
    }

    internal fun generateANRLogRecordBody(thread: Thread): String {
        val json = JSONObject()
            .put("exception.stacktrace", obtainMainThreadStackTrace())
            .put("thread.id", thread.id)
            .put("thread.name", thread.name)

        if (this::currentAppState.isInitialized) {
            json.put("android.state", currentAppState)
        }
        return json.toString()
    }

    private fun generateANRLogRecordAttributes(): Attributes {
        return Attributes.of(AttributeKey.stringKey("event.name"), ANR_EVENT_NAME)
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
}