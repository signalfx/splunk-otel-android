/*
 * Copyright 2026 Splunk Inc.
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.crash.internal.extractor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributesBuilder

/**
 * Captures details about the runtime environment (free storage, free heap, battery level) at the
 * time of a crash. The battery level is kept up to date via a broadcast receiver so it can be read
 * cheaply while the process is terminating.
 */
internal class RuntimeDetailsExtractor private constructor(private val context: Context) :
    BroadcastReceiver(),
    CrashAttributesExtractor {

    @Volatile
    private var batteryPercent: Double? = null

    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        batteryPercent = level * 100.0 / scale.toFloat()
    }

    override fun extract(attributes: AttributesBuilder, crashDetails: CrashDetails) {
        attributes.put(STORAGE_SPACE_FREE_KEY, context.filesDir.freeSpace)
        attributes.put(HEAP_FREE_KEY, Runtime.getRuntime().freeMemory())
        batteryPercent?.let { attributes.put(BATTERY_PERCENT_KEY, it) }
    }

    companion object {
        fun create(context: Context): RuntimeDetailsExtractor {
            val batteryChangedFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val runtimeDetails = RuntimeDetailsExtractor(context.applicationContext)
            context.registerReceiver(runtimeDetails, batteryChangedFilter)
            return runtimeDetails
        }

        private val STORAGE_SPACE_FREE_KEY: AttributeKey<Long> = AttributeKey.longKey("storage.free")
        private val HEAP_FREE_KEY: AttributeKey<Long> = AttributeKey.longKey("heap.free")
        private val BATTERY_PERCENT_KEY: AttributeKey<Double> = AttributeKey.doubleKey("battery.percent")
    }
}
