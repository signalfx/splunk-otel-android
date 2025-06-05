/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.splunk.app.util.CommonUtils.showDoneToast
import java.util.Locale
import java.util.Random

/**
 * Note: These slow/frozen rendering tests intentionally stress the UI thread beyond
 * normal operating limits, and may cause app crashes, ANRs, UI freezes, etc.
 * This behavior is expected and normal.
 */
object SlowRenderingUtils {

    /**
     * Replicates slow rendering by creating a view with deliberately bad drawing performance
     */
    fun simulateSlowRendering(
        fragment: Fragment,
        testName: String,
        renderDelayMs: Long,
        color: Int,
        refreshInterval: Long = 16,
        durationMs: Long = 10000
    ) {
        val slowRenderView = SlowRenderView(
            fragment.requireContext(),
            renderDelayMs,
            color,
            refreshInterval
        )

        slowRenderView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val rootView = fragment.requireActivity().window.decorView
            .findViewById<ViewGroup>(android.R.id.content)

        rootView.addView(slowRenderView)
        Toast.makeText(fragment.context, "Running $testName test for ${durationMs / 1000} seconds", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            rootView.removeView(slowRenderView)
            slowRenderView.stopRendering()
            showDoneToast(fragment.context,
                testName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
        }, durationMs)
    }

    /**
     * Custom view that does expensive drawing operations to simulate slow rendering
     */
    private class SlowRenderView(
        context: Context,
        private val renderDelayMs: Long,
        private val shapeColor: Int,
        private val refreshIntervalMs: Long = 16
    ) : View(context) {

        private val shapePaint = Paint().apply { color = shapeColor }
        private val randomGenerator = Random()
        private val renderHandler = Handler(Looper.getMainLooper())

        init {
            startSlowRendering()
        }

        private fun startSlowRendering() {
            invalidate()
            renderHandler.postDelayed({ startSlowRendering() }, refreshIntervalMs)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val renderStartTime = SystemClock.elapsedRealtime()

            // Draw until target delay time is consumed
            while (SystemClock.elapsedRealtime() - renderStartTime < renderDelayMs) {
                repeat(200) {
                    if (width > 0 && height > 0) {
                        canvas.drawCircle(
                            randomGenerator.nextFloat() * width,
                            randomGenerator.nextFloat() * height,
                            3f,
                            shapePaint
                        )
                    }
                }
            }
        }

        fun stopRendering() {
            renderHandler.removeCallbacksAndMessages(null)
        }
    }
}