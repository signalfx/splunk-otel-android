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

package com.splunk.app.ui.crashreports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.splunk.app.R
import com.splunk.app.databinding.FragmentCrashReportsBinding
import com.splunk.app.ui.BaseFragment

class CrashReportsFragment : BaseFragment<FragmentCrashReportsBinding>() {

    override val titleRes: Int = R.string.crash_reports_title

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCrashReportsBinding
        get() = FragmentCrashReportsBinding::inflate


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            crashReportsIllegal.setOnClickListener { triggerIllegalArgumentCrash() }
            crashReportsMainThread.setOnClickListener { triggerMainThreadCrash() }
            crashReportsInBackground.setOnClickListener { triggerBackgroundThreadCrash() }
            crashReportsNoAppCode.setOnClickListener { triggerNoAppCodeCrash() }
            crashReportsNoStacktrace.setOnClickListener { triggerNoStacktraceCrash() }
            crashReportsOutOfMemoryError.setOnClickListener { triggerOutOfMemoryCrash() }
            crashReportsWithChainedExceptions.setOnClickListener { triggerChainedExceptionCrash() }
            crashReportsNull.setOnClickListener { triggerNullPointerCrash() }
            anrEvent.setOnClickListener { simulateAnrEvent() }
        }
    }

    private fun triggerIllegalArgumentCrash() {
        throw IllegalArgumentException("Illegal Argument Exception Thrown!")
    }

    private fun triggerMainThreadCrash() {
        throw RuntimeException("Crashing on main thread")
    }

    private fun triggerBackgroundThreadCrash() {
        Thread({
            throw RuntimeException("Attempt to crash background thread")
        }, "CrashTestThread").start()
    }

    private fun triggerNoAppCodeCrash() {
        throw RuntimeException("No Application Code").apply {
            stackTrace = arrayOf(
                StackTraceElement("android.fake.Crash", "crashMe", "NotARealFile.kt", 12),
                StackTraceElement("android.fake.Class", "foo", "NotARealFile.kt", 34),
                StackTraceElement("android.fake.Main", "main", "NotARealFile.kt", 56)
            )
        }
    }

    private fun triggerNoStacktraceCrash() {
        throw RuntimeException("NoStackTrace").apply {
            stackTrace = arrayOfNulls(0)
        }
    }

    private fun triggerOutOfMemoryCrash() {
        throw OutOfMemoryError("Out of memory").apply {
            stackTrace = arrayOfNulls(0)
        }
    }

    private fun triggerChainedExceptionCrash() {
        try {
            throw NullPointerException("Simulated error in exception 1")
        } catch (exception: NullPointerException) {
            throw IllegalArgumentException("Simulated error in exception 2", exception)
        }
    }

    private fun triggerNullPointerCrash() {
        throw NullPointerException("I am null!")
    }

    /**
     * Simulates an ANR (Application Not Responding) event by blocking the main thread.
     * This freezes the UI for [ANR_TIMEOUT_DURATION] milliseconds.
     */
    private fun simulateAnrEvent() {
        try {
            Thread.sleep(ANR_TIMEOUT_DURATION)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private const val ANR_TIMEOUT_DURATION = 6_000L
    }
}
