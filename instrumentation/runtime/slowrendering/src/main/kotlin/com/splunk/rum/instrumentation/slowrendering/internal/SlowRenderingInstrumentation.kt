/*
 * Copyright 2026 Splunk Inc.
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.slowrendering.internal

import android.app.Application
import android.content.Context
import android.os.Build
import com.splunk.rum.common.logger.Logger
import io.opentelemetry.api.OpenTelemetry
import java.time.Duration

/**
 * Entry point for installing slow/frozen rendering detection. Only supported on Android N (API 24)
 * and newer; [install] no-ops (with a warning) on older platforms.
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
@Suppress("NewApi") // Duration APIs require API 26 or core library desugaring
class SlowRenderingInstrumentation {

    private var slowRenderingDetectionPollInterval: Duration = Duration.ofSeconds(1)

    /** Sets the frame-duration poll rate. Non-positive [interval]s are ignored. Returns `this`. */
    fun setSlowRenderingDetectionPollInterval(interval: Duration): SlowRenderingInstrumentation {
        if (interval.toMillis() <= 0) {
            Logger.e(TAG, "Invalid slowRenderingDetectionPollInterval: $interval; must be positive")
            return this
        }
        slowRenderingDetectionPollInterval = interval
        return this
    }

    /** Registers the frame-metrics listeners and starts polling. No-op below API 24. */
    fun install(context: Context, openTelemetry: OpenTelemetry) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Logger.w(
                TAG,
                "Slow/frozen rendering detection is not supported on platforms older than Android N (SDK version 24)."
            )
            return
        }

        val application = context.applicationContext as Application
        val detector = SlowRenderListener(
            openTelemetry.getTracer(INSTRUMENTATION_SCOPE_NAME),
            slowRenderingDetectionPollInterval
        )

        application.registerActivityLifecycleCallbacks(detector)
        detector.start()
    }

    companion object {
        private const val TAG = "SlowRendering"

        const val INSTRUMENTATION_SCOPE_NAME = "com.splunk.rum.slowrendering"
    }
}
