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

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.SparseIntArray
import android.view.FrameMetrics
import android.view.Window
import com.splunk.android.common.logger.Logger
import io.opentelemetry.api.trace.Tracer
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Polls per-activity frame render durations and reports slow/frozen frame counts as spans.
 *
 * A single background [HandlerThread] receives frame metrics for every resumed activity; a scheduled
 * executor drains the per-activity histograms on the configured [pollInterval]. Histograms are also
 * drained (and reported) when an activity is paused so nothing is lost on navigation.
 */
// FrameMetrics APIs require API 24; install() guards on SDK_INT before this listener is created.
@Suppress("NewApi")
internal class SlowRenderListener internal constructor(
    private val tracer: Tracer,
    private val executorService: ScheduledExecutorService,
    private val frameMetricsHandler: Handler,
    private val pollInterval: Duration
) : Application.ActivityLifecycleCallbacks {

    constructor(tracer: Tracer, pollInterval: Duration) : this(
        tracer,
        Executors.newScheduledThreadPool(1),
        Handler(startFrameMetricsLoop()),
        pollInterval
    )

    private val activities = ConcurrentHashMap<Activity, PerActivityListener>()

    fun start() {
        executorService.scheduleWithFixedDelay(
            ::reportSlowRenders,
            pollInterval.toMillis(),
            pollInterval.toMillis(),
            TimeUnit.MILLISECONDS
        )
    }

    override fun onActivityResumed(activity: Activity) {
        val listener = PerActivityListener(activity)
        val existing = activities.putIfAbsent(activity, listener)
        if (existing == null) {
            activity.window.addOnFrameMetricsAvailableListener(listener, frameMetricsHandler)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        val listener = activities.remove(activity)
        if (listener != null) {
            activity.window.removeOnFrameMetricsAvailableListener(listener)
            reportSlow(listener)
        }
    }

    private fun reportSlowRenders() {
        try {
            for (listener in activities.values) {
                reportSlow(listener)
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Exception while processing frame metrics: ${e.message}")
        }
    }

    private fun reportSlow(listener: PerActivityListener) {
        var slowCount = 0
        var frozenCount = 0
        val durationToCountHistogram = listener.resetMetrics()
        for (i in 0 until durationToCountHistogram.size()) {
            val duration = durationToCountHistogram.keyAt(i)
            val count = durationToCountHistogram.get(duration)
            when {
                duration > FROZEN_THRESHOLD_MS -> {
                    Logger.d(TAG, "* FROZEN RENDER DETECTED: $duration ms. $count times")
                    frozenCount += count
                }
                duration > SLOW_THRESHOLD_MS -> {
                    Logger.d(TAG, "* Slow render detected: $duration ms. $count times")
                    slowCount += count
                }
            }
        }

        val now = Instant.now()
        if (slowCount > 0) {
            makeSpan(SLOW_RENDERS_SPAN_NAME, listener.activityName, slowCount, now)
        }
        if (frozenCount > 0) {
            makeSpan(FROZEN_RENDERS_SPAN_NAME, listener.activityName, frozenCount, now)
        }
    }

    private fun makeSpan(spanName: String, activityName: String, count: Int, now: Instant) {
        val span = tracer.spanBuilder(spanName)
            .setAttribute(COUNT_ATTRIBUTE_KEY, count.toLong())
            .setAttribute(ACTIVITY_NAME_ATTRIBUTE_KEY, activityName)
            .setStartTimestamp(now)
            .startSpan()
        span.end(now)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    @Suppress("NewApi")
    internal class PerActivityListener(private val activity: Activity) : Window.OnFrameMetricsAvailableListener {

        private val lock = Any()

        @Volatile
        private var drawDurationHistogram = SparseIntArray()

        val activityName: String
            get() = activity.componentName.flattenToShortString()

        override fun onFrameMetricsAvailable(
            window: Window,
            frameMetrics: FrameMetrics,
            dropCountSinceLastInvocation: Int
        ) {
            val firstDrawFrame = frameMetrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME)
            if (firstDrawFrame == 1L) {
                return
            }

            val drawDurationsNs = frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)
            // Ignore values < 0; something must have gone wrong.
            if (drawDurationsNs >= 0) {
                synchronized(lock) {
                    // Calculation copied from FrameMetricsAggregator (round to nearest ms).
                    val durationMs = ((drawDurationsNs + NANOS_ROUNDING_VALUE) / NANOS_PER_MS).toInt()
                    val oldValue = drawDurationHistogram.get(durationMs)
                    drawDurationHistogram.put(durationMs, oldValue + 1)
                }
            }
        }

        fun resetMetrics(): SparseIntArray {
            synchronized(lock) {
                val metrics = drawDurationHistogram
                drawDurationHistogram = SparseIntArray()
                return metrics
            }
        }

        private companion object {
            private val NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1).toInt()

            // Rounding value adds half a millisecond, for rounding to nearest ms.
            private val NANOS_ROUNDING_VALUE = NANOS_PER_MS / 2
        }
    }

    companion object {
        internal const val SLOW_THRESHOLD_MS = 16
        internal const val FROZEN_THRESHOLD_MS = 700

        const val SLOW_RENDERS_SPAN_NAME = "slowRenders"
        const val FROZEN_RENDERS_SPAN_NAME = "frozenRenders"

        private const val COUNT_ATTRIBUTE_KEY = "count"
        private const val ACTIVITY_NAME_ATTRIBUTE_KEY = "activity.name"
        private const val TAG = "SlowRendering"

        private val frameMetricsThread = HandlerThread("FrameMetricsCollector")

        private fun startFrameMetricsLoop(): Looper {
            // Precaution: this is meant to be called once and the thread should not yet be started.
            if (!frameMetricsThread.isAlive) {
                frameMetricsThread.start()
            }
            return frameMetricsThread.looper
        }
    }
}
