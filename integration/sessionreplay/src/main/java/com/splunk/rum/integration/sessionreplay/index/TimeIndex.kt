package com.splunk.rum.integration.sessionreplay.index

import java.time.Instant
import java.util.TreeMap

internal class TimeIndex<T> {
    private val timeMap = TreeMap<Instant, T>()

    fun put(value: T) {
        timeMap[Instant.now()] = value
    }

    fun putAt(time: Instant, value: T) {
        timeMap[time] = value
    }

    fun getAt(time: Instant): T? = timeMap.floorEntry(time)?.value
}
