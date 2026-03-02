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
import com.splunk.android.common.logger.Logger
import com.splunk.rum.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.navigation.RumConstant
import com.splunk.rum.integration.navigation.tracer.fragment.FragmentTracerManager

internal class FragmentCallback(private val manager: FragmentTracerManager) :
    FragmentManager.FragmentLifecycleCallbacks() {

    override fun onFragmentPreAttached(fm: FragmentManager, f: Fragment, context: Context) {
        Logger.d("FragmentCallback", "onFragmentPreAttached")
        manager.getTracer(f)
            .startFragmentCreation()
            .addEvent(RumConstant.NAVIGATION_FRAGMENT_PRE_ATTACHED_EVENT)
    }

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        Logger.d("FragmentCallback", "onFragmentAttached")
        manager.addEvent(f, RumConstant.NAVIGATION_FRAGMENT_ATTACHED_EVENT)
    }

    override fun onFragmentPreCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        Logger.d("FragmentCallback", "onFragmentPreCreated")
        manager.addEvent(f, RumConstant.NAVIGATION_FRAGMENT_PRE_CREATED_EVENT)
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        Logger.d("FragmentCallback", "onFragmentCreated")
        manager.addEvent(f, RumConstant.NAVIGATION_FRAGMENT_CREATED_EVENT)
    }

    override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
        Logger.d("FragmentCallback", "onFragmentViewCreated")
        manager.getTracer(f)
            .startSpanIfNoneInProgress(RumConstant.NAVIGATION_RESTORED_SPAN_NAME)
            .addEvent(RumConstant.NAVIGATION_FRAGMENT_VIEW_CREATED_EVENT)
    }

    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentStarted")
        manager.addEvent(f, RumConstant.NAVIGATION_FRAGMENT_STARTED_EVENT)
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentResumed")
        manager.getTracer(f)
            .startSpanIfNoneInProgress(RumConstant.NAVIGATION_RESUMED_SPAN_NAME)
            .addEvent(RumConstant.NAVIGATION_FRAGMENT_RESUMED_EVENT)
            .endActiveSpan()
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentPaused")
        manager.getTracer(f)
            .startSpanIfNoneInProgress(RumConstant.NAVIGATION_PAUSED_SPAN_NAME)
            .addEvent(RumConstant.NAVIGATION_FRAGMENT_PAUSED_EVENT)
            .endActiveSpan()
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentStopped")
        manager.getTracer(f)
            .startSpanIfNoneInProgress(RumConstant.NAVIGATION_STOPPED_SPAN_NAME)
            .addEvent(RumConstant.NAVIGATION_FRAGMENT_STOPPED_EVENT)
            .endActiveSpan()
    }

    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentViewDestroyed")
        manager.getTracer(f)
            .startSpanIfNoneInProgress(RumConstant.NAVIGATION_VIEW_DESTROYED_SPAN_NAME)
            .addEvent(RumConstant.NAVIGATION_FRAGMENT_VIEW_DESTROYED_EVENT)
            .endActiveSpan()
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentDestroyed")
        manager.getTracer(f)
            .startSpanIfNoneInProgress(RumConstant.NAVIGATION_DESTROYED_SPAN_NAME)
            .addEvent(RumConstant.NAVIGATION_FRAGMENT_DESTROYED_EVENT)
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        Logger.d("FragmentCallback", "onFragmentDetached")
        manager.getTracer(f)
            .startSpanIfNoneInProgress(RumConstant.NAVIGATION_DETACHED_SPAN_NAME)
            .addEvent(RumConstant.NAVIGATION_FRAGMENT_DETACHED_EVENT)
            .endActiveSpan()
    }
}
