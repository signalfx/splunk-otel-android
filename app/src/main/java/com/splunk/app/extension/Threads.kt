package com.splunk.app.extension

import android.os.Handler
import android.os.Looper

private val mainHandler = Handler(Looper.getMainLooper())

fun runOnUiThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper())
        block()
    else
        mainHandler.post(block)
}