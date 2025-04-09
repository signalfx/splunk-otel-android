package com.splunk.rum.integration.lifecycle.tracer.fragment.callback

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.splunk.rum.integration.lifecycle.tracer.fragment.FragmentTracerManager

internal class FragmentCallback(
    private val manager: FragmentTracerManager
) : FragmentManager.FragmentLifecycleCallbacks() {

    override fun onFragmentPreAttached(fm: FragmentManager, f: Fragment, context: Context) {
        manager.getTracer(f)
            .startFragmentCreation()
            .addEvent("fragmentPreAttached")
    }

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        manager.addEvent(f, "fragmentAttached")
    }

    override fun onFragmentPreCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        manager.addEvent(f, "fragmentPreCreated")
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        manager.addEvent(f, "fragmentCreated")
    }

    override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Restored")
            .addEvent("fragmentViewCreated")
    }

    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        manager.addEvent(f, "fragmentStarted")
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Resumed")
            .addEvent("onFragmentResumed")
            .addPreviousScreenAttribute()
            .endActiveSpan()
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Paused")
            .addEvent("onFragmentPaused")
            .endActiveSpan()
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Stopped")
            .addEvent("fragmentStopped")
            .endActiveSpan()
    }

    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        manager.getTracer(f)
            .startSpanIfNoneInProgress("ViewDestroyed")
            .addEvent("fragmentViewDestroyed")
            .endActiveSpan()
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Destroyed")
            .addEvent("fragmentDestroyed")
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        manager.getTracer(f)
            .startSpanIfNoneInProgress("Detached")
            .addEvent("fragmentDetached")
            .endActiveSpan()
    }
}
