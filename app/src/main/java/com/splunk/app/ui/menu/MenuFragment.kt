/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.app.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.cisco.android.common.utils.runOnUiThread
import com.splunk.app.App
import com.splunk.app.R
import com.splunk.app.databinding.FragmentMenuBinding
import com.splunk.app.ui.BaseFragment
import com.splunk.app.ui.httpurlconnection.HttpURLConnectionFragment
import com.splunk.app.ui.okhttp.OkHttpFragment
import com.splunk.app.util.FragmentAnimation
import com.splunk.rum.customtracking.extension.customTracking
import com.splunk.rum.integration.agent.api.SplunkRUMAgent
import io.opentelemetry.api.common.Attributes
import com.splunk.rum.integration.agent.api.extension.splunkRumId

class MenuFragment : BaseFragment<FragmentMenuBinding>() {

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentMenuBinding
        get() = FragmentMenuBinding::inflate

    override val titleRes: Int = R.string.menu_title

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.crashReportsIllegal.setOnClickListener(onClickListener)
        viewBinding.crashReportsMainThread.setOnClickListener(onClickListener)
        viewBinding.crashReportsInBackground.setOnClickListener(onClickListener)
        viewBinding.crashReportsNoAppCode.setOnClickListener(onClickListener)
        viewBinding.crashReportsNoStacktrace.setOnClickListener(onClickListener)
        viewBinding.crashReportsOutOfMemoryError.setOnClickListener(onClickListener)
        viewBinding.crashReportsWithChainedExceptions.setOnClickListener(onClickListener)
        viewBinding.crashReportsNull.setOnClickListener(onClickListener)
        viewBinding.anrEvent.setOnClickListener(onClickListener)
        viewBinding.okhttpSampleCalls.setOnClickListener(onClickListener)
        viewBinding.httpurlconnection.setOnClickListener(onClickListener)
        viewBinding.trackCustomEvent.setOnClickListener(onClickListener)
        viewBinding.trackWorkflow.setOnClickListener(onClickListener)
        viewBinding.crashReportsIllegal.splunkRumId = "illegalButton"
    }

    private val onClickListener = View.OnClickListener {
        when (it.id) {
            viewBinding.crashReportsIllegal.id ->
                throw IllegalArgumentException("Illegal Argument Exception Thrown!")

            viewBinding.crashReportsMainThread.id ->
                throw RuntimeException("Crashing on main thread")

            viewBinding.crashReportsInBackground.id ->
                Thread { throw RuntimeException("Attempt to crash background thread") }.start()

            viewBinding.crashReportsNoAppCode.id -> {
                val e = java.lang.RuntimeException("No Application Code")
                e.stackTrace = arrayOf(
                    StackTraceElement("android.fake.Crash", "crashMe", "NotARealFile.kt", 12),
                    StackTraceElement("android.fake.Class", "foo", "NotARealFile.kt", 34),
                    StackTraceElement("android.fake.Main", "main", "NotARealFile.kt", 56)
                )
                throw e
            }

            viewBinding.crashReportsNoStacktrace.id -> {
                val e = java.lang.RuntimeException("No Stack Trace")
                e.stackTrace = arrayOfNulls(0)
                throw e
            }

            viewBinding.crashReportsOutOfMemoryError.id -> {
                val e = OutOfMemoryError("out of memory")
                e.stackTrace = arrayOfNulls(0)
                throw e
            }

            viewBinding.crashReportsWithChainedExceptions.id -> {
                try {
                    throw NullPointerException("Simulated error in exception 1")
                } catch (e: NullPointerException) {
                    throw IllegalArgumentException("Simulated error in exception 2", e)
                }
            }

            viewBinding.crashReportsNull.id ->
                throw NullPointerException("I am null!")

            viewBinding.anrEvent.id -> {
                try {
                    Thread.sleep(6000)
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }

            viewBinding.okhttpSampleCalls.id ->
                navigateTo(OkHttpFragment(), FragmentAnimation.FADE)

            viewBinding.httpurlconnection.id ->
                navigateTo(HttpURLConnectionFragment(), FragmentAnimation.FADE)

            viewBinding.trackCustomEvent.id -> {
                val testAttributes = Attributes.builder()
                    .put("attribute.one", "value1")
                    .put("attribute.two", "12345")
                    .build()
                getAgent()?.customTracking?.trackCustomEvent("TestEvent", testAttributes)
                showDoneToast("Track Custom Event")
            }

            viewBinding.trackWorkflow.id -> {
                val workflowSpan = getAgent()?.customTracking?.trackWorkflow("Test Workflow")
                workflowSpan?.setAttribute("workflow.start.time", System.currentTimeMillis())
                // Simulate some processing time
                Thread.sleep(125)
                workflowSpan?.setAttribute("workflow.end.time", System.currentTimeMillis())
                workflowSpan?.end()
                showDoneToast("Track Workflow")
            }
        }
    }

    private fun getAgent(): SplunkRUMAgent? {
        val app = requireActivity().application as? App
        return app?.agent
    }

    private fun showDoneToast(name: String) {
        runOnUiThread {
            Toast.makeText(context, "$name, Done!", Toast.LENGTH_SHORT).show()
        }
    }
}
