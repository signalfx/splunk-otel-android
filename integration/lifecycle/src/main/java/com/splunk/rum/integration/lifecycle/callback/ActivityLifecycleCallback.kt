/*
 * Copyright 2026 Splunk Inc.
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
 * Activity lifecycle callbacks for all API levels.
 * Pre/post callbacks (API 29+) will only be invoked by the system on API 29+.
 */
internal class ActivityLifecycleCallback(private val emitter: LifecycleEventEmitter) :
    Application.ActivityLifecycleCallbacks {

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityLifecycleCallback", "onActivityPreCreated: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.PRE_CREATED)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityLifecycleCallback", "onActivityCreated: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.CREATED)
    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityLifecycleCallback", "onActivityPostCreated: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.POST_CREATED)
    }

    override fun onActivityPreStarted(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPreStarted: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.PRE_STARTED)
    }

    override fun onActivityStarted(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityStarted: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.STARTED)
    }

    override fun onActivityPostStarted(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPostStarted: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.POST_STARTED)
    }

    override fun onActivityPreResumed(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPreResumed: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.PRE_RESUMED)
    }

    override fun onActivityResumed(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityResumed: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.RESUMED)
    }

    override fun onActivityPostResumed(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPostResumed: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.POST_RESUMED)
    }

    override fun onActivityPrePaused(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPrePaused: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.PRE_PAUSED)
    }

    override fun onActivityPaused(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPaused: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.PAUSED)
    }

    override fun onActivityPostPaused(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPostPaused: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.POST_PAUSED)
    }

    override fun onActivityPreStopped(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPreStopped: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.PRE_STOPPED)
    }

    override fun onActivityStopped(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityStopped: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.STOPPED)
    }

    override fun onActivityPostStopped(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPostStopped: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.POST_STOPPED)
    }

    override fun onActivityPreDestroyed(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPreDestroyed: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.PRE_DESTROYED)
    }

    override fun onActivityDestroyed(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityDestroyed: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.DESTROYED)
    }

    override fun onActivityPostDestroyed(activity: Activity) {
        Logger.d("ActivityLifecycleCallback", "onActivityPostDestroyed: ${activity::class.java.simpleName}")
        emitter.emitActivityEvent(activity, LifecycleAction.POST_DESTROYED)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Not tracked as a lifecycle event
    }
}
