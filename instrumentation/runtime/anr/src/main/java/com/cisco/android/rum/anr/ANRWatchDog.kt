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

import android.os.Debug
import android.os.Handler
import com.cisco.android.common.logger.Logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ANRWatchDog(
    private val uiHandler: Handler,
    internal val anrListener: ANRListener,
    private val thresholdSeconds: Long = DEFAULT_ANR_TIMEOUT,
    private val ignoreDebugger: Boolean
) : Runnable {

    companion object {
        private val anrCounter = AtomicInteger()
        private const val TAG = "ANRReportingHandler"
        private val DEFAULT_ANR_TIMEOUT = 5L
    }

    override fun run() {
        val response = CountDownLatch(1)
        val postedSuccessfully = uiHandler.post {
            response.countDown()
        }
        if (!postedSuccessfully) {
            Logger.i(TAG, "ANR detection operation cannot be posted to UI as UI thread is possibly shutting down")
            return
        }

        val success: Boolean = try {
            response.await(100, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Logger.w(TAG, "ANR Detector was interrupted with exception ${e.message}")
            return
        }

        if (success) {
            anrCounter.set(0)
            return
        }

        if (anrCounter.incrementAndGet() >= thresholdSeconds) {
            // TODO: IgnoreDebugger is currently always set to true.
            //  We should decide whether or not to expose this to the customer and update the logging if so
            if (!ignoreDebugger && (Debug.isDebuggerConnected() || Debug.waitingForDebugger())) {
                Logger.w(TAG, "An ANR was detected but ignored because the debugger is connected (you can prevent this by changing ignoreDebugger to true)")
                return
            } else {
                anrCounter.set(0)
                anrListener.onAppNotResponding()
            }
        }
    }

    interface ANRListener {
        fun onAppNotResponding()
    }
}
