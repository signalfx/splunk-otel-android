package com.splunk.sdk.common.utils

class Lock(isLocked: Boolean = false) {

    private val barrier = Barrier()

    init {
        if (isLocked)
            lock()
    }

    fun lock() {
        barrier.set(1)
    }

    fun unlock() {
        barrier.set(0)
    }

    fun isLocked(): Boolean {
        return barrier.getLockCount() > 0
    }

    fun waitToUnlock() {
        barrier.waitToComplete()
    }

    override fun toString(): String {
        return "Lock(isLocked: ${isLocked()})"
    }
}
