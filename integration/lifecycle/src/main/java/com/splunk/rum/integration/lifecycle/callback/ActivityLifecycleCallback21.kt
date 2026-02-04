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

package com.splunk.rum.integration.lifecycle.callback

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.lifecycle.LifecycleEventEmitter
import com.splunk.rum.integration.lifecycle.model.LifecycleAction

/**
 * Activity lifecycle callbacks for API 21-28.
 * Pattern copied from: navigation/tracer/activity/callback/ActivityCallback21.kt
 * Simplified: Just emit events, no span management.
 */
internal class ActivityLifecycleCallback21(private val emitter: LifecycleEventEmitter) :
    Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityLifecycleCallback21", "onActivityCreated: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.CREATED)
    }

    override fun onActivityStarted(activity: Activity) {
        Logger.d("ActivityLifecycleCallback21", "onActivityStarted: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.STARTED)
    }

    override fun onActivityResumed(activity: Activity) {
        Logger.d("ActivityLifecycleCallback21", "onActivityResumed: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.RESUMED)
    }

    override fun onActivityPaused(activity: Activity) {
        Logger.d("ActivityLifecycleCallback21", "onActivityPaused: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.PAUSED)
    }

    override fun onActivityStopped(activity: Activity) {
        Logger.d("ActivityLifecycleCallback21", "onActivityStopped: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.STOPPED)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Not tracked as a lifecycle event
    }

    override fun onActivityDestroyed(activity: Activity) {
        Logger.d("ActivityLifecycleCallback21", "onActivityDestroyed: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.DESTROYED)
    }
}
