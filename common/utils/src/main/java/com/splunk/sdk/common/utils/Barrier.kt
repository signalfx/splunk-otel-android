package com.splunk.sdk.common.utils

class Barrier(
    @Volatile private var lockCount: Int = 0
) {

    private val lock = Object()

    fun increase() {
        synchronized(lock) {
            lockCount++
        }
    }

    operator fun plusAssign(value: Int) {
        synchronized(lock) {
            lockCount += value
        }
    }

    fun decrease() {
        synchronized(lock) {
            lockCount--

            if (lockCount < 0)
                lockCount = 0

            checkIfCompleted()
        }
    }

    fun set(count: Int) {
        synchronized(lock) {
            lockCount = count.coerceAtLeast(0)
            checkIfCompleted()
        }
    }

    fun waitToComplete() {
        synchronized(lock) {
            if (!checkIfCompleted())
                runCatching { lock.wait() }
        }
    }

    fun getLockCount(): Int {
        synchronized(lock) {
            return lockCount
        }
    }

    private fun checkIfCompleted(): Boolean {
        synchronized(lock) {
            if (lockCount == 0) {
                lock.notifyAll()
                return true
            }

            return false
        }
    }

    override fun toString(): String {
        return "Barrier(lockCount: $lockCount)"
    }
}
