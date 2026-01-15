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

package com.splunk.rum.integration.navigation.tracer.fragment.callback

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.splunk.rum.integration.navigation.tracer.fragment.FragmentTracerManager
import com.splunk.android.common.logger.Logger

internal class FragmentCallback(private val manager: FragmentTracerManager) :
    FragmentManager.FragmentLifecycleCallbacks() {

    override fun onFragmentPreAttached(fm: FragmentManager, f: Fragment, context: Context) {
        Logger.d("FragmentCallback", "onFragmentPreAttached")
        manager.getTracer(f)
            .startFragmentCreation()
            .addEvent("fragmentPreAttached")
    }

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        Logger.d("FragmentCallback", "onFragmentAttached")
        manager.addEvent(f, "fragmentAttached")
    }

    override fun onFragmentPreCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        Logger.d("FragmentCallback", "onFragmentPreCreated")
        manager.addEvent(f, "fragmentPreCreated")
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        Logger.d("FragmentCallback", "onFragmentCreated")
        manager.addEvent(f, "fragmentCreated")
    }

    override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
        Logger.d("FragmentCallback", "onFragmentViewCreated")
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Restored")
            .addEvent("fragmentViewCreated")
    }

    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentStarted")
        manager.addEvent(f, "fragmentStarted")
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentResumed")
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Resumed")
            .addEvent("onFragmentResumed")
            .endActiveSpan()
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentPaused")
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Paused")
            .addEvent("onFragmentPaused")
            .endActiveSpan()
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentStopped")
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Stopped")
            .addEvent("fragmentStopped")
            .endActiveSpan()
    }

    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentViewDestroyed")
        manager.getTracer(f)
            .startSpanIfNoneInProgress("ViewDestroyed")
            .addEvent("fragmentViewDestroyed")
            .endActiveSpan()
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentDestroyed")
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Destroyed")
            .addEvent("fragmentDestroyed")
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentDetached")
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Detached")
            .addEvent("fragmentDetached")
            .endActiveSpan()
    }
}
