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

package com.splunk.app.extension

import android.os.Handler
import android.os.Looper

private val mainHandler = Handler(Looper.getMainLooper())

fun runOnUiThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block()
    } else {
        mainHandler.post(block)
    }
}

fun runOnBackgroundThread(
    name: String = "BackgroundWorker",
    uncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null,
    block: () -> Unit
): Thread {
    val thread = Thread(block, name)
    thread.uncaughtExceptionHandler = uncaughtExceptionHandler
    thread.start()
    return thread
}
