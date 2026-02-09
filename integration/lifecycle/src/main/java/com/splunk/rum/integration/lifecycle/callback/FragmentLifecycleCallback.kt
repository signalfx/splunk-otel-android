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

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.lifecycle.LifecycleEventEmitter
import com.splunk.rum.integration.lifecycle.model.LifecycleAction

internal class FragmentLifecycleCallback(private val emitter: LifecycleEventEmitter) :
    FragmentManager.FragmentLifecycleCallbacks() {

    override fun onFragmentPreAttached(fm: FragmentManager, f: Fragment, context: Context) {
        Logger.d("FragmentLifecycleCallback", "onFragmentPreAttached: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.PRE_ATTACHED)
    }

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        Logger.d("FragmentLifecycleCallback", "onFragmentAttached: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.ATTACHED)
    }

    override fun onFragmentPreCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        Logger.d("FragmentLifecycleCallback", "onFragmentPreCreated: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.PRE_CREATED)
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        Logger.d("FragmentLifecycleCallback", "onFragmentCreated: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.CREATED)
    }

    override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
        Logger.d("FragmentLifecycleCallback", "onFragmentViewCreated: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.VIEW_CREATED)
    }

    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentLifecycleCallback", "onFragmentStarted: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.STARTED)
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentLifecycleCallback", "onFragmentResumed: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.RESUMED)
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentLifecycleCallback", "onFragmentPaused: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.PAUSED)
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentLifecycleCallback", "onFragmentStopped: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.STOPPED)
    }

    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentLifecycleCallback", "onFragmentViewDestroyed: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.VIEW_DESTROYED)
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentLifecycleCallback", "onFragmentDestroyed: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.DESTROYED)
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentLifecycleCallback", "onFragmentDetached: ${f::class.java.simpleName}")
        emitter.emitFragmentEvent(f, LifecycleAction.DETACHED)
    }
}
