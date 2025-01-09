package com.splunk.sdk.common.utils.extensions

import com.splunk.sdk.common.utils.runOnUiThread
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

fun ScheduledExecutorService.safeSchedule(delayMs: Long, action: () -> Unit): ScheduledFuture<*> {
    val safeAction = {
        try {
            action()
        } catch (e: Exception) {
            val delegatedException = Exception("Exception catch in '${Thread.currentThread().name}' thread", e)
            runOnUiThread { throw delegatedException }
        }
    }

    return schedule(safeAction, delayMs, TimeUnit.MILLISECONDS)
}
