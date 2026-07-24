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

package com.splunk.rum.instrumentation.slowrendering

import android.app.Application
import android.content.Context
import android.os.Build
import com.splunk.android.common.logger.Logger
import io.opentelemetry.api.OpenTelemetry
import java.time.Duration

/**
 * Entry point for installing slow/frozen rendering detection.
 *
 * Detection polls per-activity frame render durations on a background thread and emits zero-duration
 * spans summarising slow and frozen frames. It is only supported on Android N (API 24) and newer;
 * on older platforms [install] logs a warning and returns without registering anything.
 */
@Suppress("NewApi") // Duration.ofSeconds()/toMillis() require API 26 or core library desugaring
class SlowRenderingInstrumentation {

    private var slowRenderingDetectionPollInterval: Duration = Duration.ofSeconds(1)

    /**
     * Configures the rate at which frame render durations are polled.
     *
     * A non-positive [interval] is ignored (the previous value is kept).
     *
     * @return `this`, for chaining.
     */
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
