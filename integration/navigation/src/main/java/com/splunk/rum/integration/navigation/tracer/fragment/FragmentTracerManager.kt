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

package com.splunk.rum.integration.navigation.tracer.fragment

import androidx.fragment.app.Fragment
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.navigation.descriptor.ScreenNameDescriptor
import com.splunk.rum.integration.navigation.tracer.ActiveSpan
import io.opentelemetry.api.trace.Tracer

internal class FragmentTracerManager(private val tracer: Tracer) {

    private val tracers = HashMap<String, FragmentTracer>()

    fun addEvent(fragment: Fragment, eventName: String) {
        Logger.d("FragmentTracerManager", "addEvent: ${fragment.javaClass.name} -> $eventName")
        tracers[fragment.javaClass.name]?.addEvent(eventName)
    }

    fun getTracer(fragment: Fragment): FragmentTracer {
        val className = fragment.javaClass.name
        var activityTracer = tracers[className]

        if (activityTracer == null) {
            Logger.d("FragmentTracerManager", "create tracer for $className")
            activityTracer = FragmentTracer(
                fragmentName = fragment::class.java.simpleName,
                screenName = ScreenNameDescriptor.getName(fragment),
                tracer = tracer,
                activeSpan = ActiveSpan()
            )

            tracers[className] = activityTracer
        }

        return activityTracer
    }
}
