package com.smartlook.sdk.common.utils.extensions

import com.smartlook.sdk.common.utils.runOnUiThread
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

fun ExecutorService.safeSubmit(block: () -> Unit): Future<*> {
    val safeBlock = {
        try {
            block()
        } catch (e: Throwable) {
            val delegatedException = Exception("Exception catch in '${Thread.currentThread().name}' thread", e)
            runOnUiThread { throw delegatedException }
        }
    }

    return submit(safeBlock)
}
