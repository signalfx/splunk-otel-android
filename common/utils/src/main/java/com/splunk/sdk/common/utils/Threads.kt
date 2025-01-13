package com.splunk.sdk.common.utils

import android.os.Handler
import android.os.Looper

private val mainHandler = Handler(Looper.getMainLooper())

fun runOnUiThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper())
        block()
    else
        mainHandler.post(block)
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
