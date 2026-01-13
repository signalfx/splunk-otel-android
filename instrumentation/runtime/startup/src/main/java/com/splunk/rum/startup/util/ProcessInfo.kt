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

package com.splunk.rum.startup.util

import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import com.splunk.android.common.logger.Logger
import java.io.BufferedReader
import java.io.FileReader

internal object ProcessInfo {

    private const val TAG = "ProcessInfo"

    fun getStartUptimeMillis(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Process.getStartUptimeMillis()
        else
            getStartUptimeMillis20()
    }

    /**
     * Read process start time from '/proc/self/stat'.
     *
     * @see https://manpages.ubuntu.com/manpages/noble/man5/proc_pid_stat.5.html
     */
    private fun getStartUptimeMillis20(): Long {
        try {
            val reader = BufferedReader(FileReader("/proc/self/stat"))
            val stat = reader.readLine()
            reader.close()

            if (stat != null) {
                val fields = stat.split(" ")
                val startTicks = fields[21].toLong()
                val clockTicksPerSecond = Os.sysconf(OsConstants._SC_CLK_TCK)

                return (startTicks * 1000L) / clockTicksPerSecond
            }
        } catch (e: Exception) {
            Logger.e(TAG, "getStartUptimeMillis20()", e)
        }

        return SystemClock.uptimeMillis()
    }
}
