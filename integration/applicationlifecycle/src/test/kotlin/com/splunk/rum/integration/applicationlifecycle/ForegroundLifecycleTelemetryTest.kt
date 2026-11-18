/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.applicationlifecycle

import android.app.Application
import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.APP_STATE_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SESSION_ID_KEY
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import com.splunk.rum.integration.agent.internal.session.SessionSnapshot
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager
import com.splunk.rum.integration.applicationlifecycle.model.AppState
import com.splunk.rum.integration.applicationlifecycle.model.ApplicationLifecycleData
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ForegroundLifecycleTelemetryTest {
    private val exported = CopyOnWriteArrayList<LogRecordData>()

    private val manager = object : ISplunkSessionManager {
        override val allowBackgroundSessionCreation = false
        var currentId: String? = null
        override val sessionId: String? get() = currentId
        override val sessionStart: Long? get() = null
        override val sessionLastActivity: Long? get() = null
        override val sessionSnapshot: SessionSnapshot? get() = null
        override val previousSessionId: String? get() = null
        override val sessionListeners = mutableSetOf<SplunkSessionManager.SessionListener>()
        override fun install(application: Application) = Unit
        override fun trackSessionActivity() = Unit
        override fun reset() = Unit
        override fun sessionId(timestamp: Long): String? = currentId
    }

    @After
    fun tearDown() {
        SplunkOpenTelemetrySdk.instance = null
        AppStateObserver.detach()
        AppStateObserver.listeners.clear()
        ApplicationLifecycleModuleIntegration.disableAndRemoveListener()
    }

    @Test
    fun `early lifecycle events keep their timestamps and foreground session`() {
        val exporter = object : LogRecordExporter {
            override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
                exported.addAll(logs)
                return CompletableResultCode.ofSuccess()
            }

            override fun flush() = CompletableResultCode.ofSuccess()
            override fun shutdown() = CompletableResultCode.ofSuccess()
        }
        val provider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
            .build()
        val sdk = OpenTelemetrySdk.builder().setLoggerProvider(provider).build()
        SplunkOpenTelemetrySdk.instance = sdk

        val application: Application = RuntimeEnvironment.getApplication()
        ApplicationLifecycleModuleIntegration.attach(application)
        AgentIntegration.obtainInstance(application).install(
            application,
            sdk,
            manager,
            listOf(ApplicationLifecycleModuleConfiguration()),
            Attributes.empty()
        )

        ApplicationLifecycleModuleIntegration.reportEvent(ApplicationLifecycleData(1_000L, AppState.CREATED))
        ApplicationLifecycleModuleIntegration.reportEvent(ApplicationLifecycleData(2_000L, AppState.FOREGROUND))
        assertTrue(exported.isEmpty())

        manager.currentId = "A"
        manager.sessionListeners.forEach { it.onSessionChanged("A", 3_000L) }

        assertEquals(listOf("A", "A"), exported.map { it.attributes.get(SESSION_ID_KEY) })
        assertEquals(listOf(1_000L, 2_000L), exported.map { TimeUnit.NANOSECONDS.toMillis(it.timestampEpochNanos) })
        assertEquals(listOf("created", "foreground"), exported.map { it.attributes.get(APP_STATE_KEY) })

        manager.currentId = null
        ApplicationLifecycleModuleIntegration.reportEvent(ApplicationLifecycleData(4_000L, AppState.CREATED))
        ApplicationLifecycleModuleIntegration.appStateListenerReference.onAppClosed()
        manager.currentId = "B"
        manager.sessionListeners.forEach { it.onSessionChanged("B", 5_000L) }
        assertEquals(2, exported.size)

        provider.shutdown().join(5, TimeUnit.SECONDS)
    }
}
