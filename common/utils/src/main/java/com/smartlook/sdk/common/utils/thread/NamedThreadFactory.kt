package com.smartlook.sdk.common.utils.thread

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(
    private val name: String
) : ThreadFactory {

    private val index = AtomicInteger()

    override fun newThread(runnable: Runnable): Thread {
        val thread = Thread(runnable)
        thread.name = "${name}_${index.incrementAndGet()}"
        return thread
    }
}
