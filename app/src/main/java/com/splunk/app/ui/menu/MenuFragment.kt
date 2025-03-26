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

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.cisco.android.common.utils.runOnUiThread
import com.splunk.app.R
import com.splunk.app.databinding.FragmentMenuBinding
import com.splunk.app.ui.BaseFragment
import com.splunk.app.ui.httpurlconnection.HttpURLConnectionFragment
import com.splunk.app.ui.okhttp.OkHttpFragment
import com.splunk.app.util.FragmentAnimation
import com.splunk.rum.customtracking.extension.customTracking
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.agent.api.attributes.extension.globalAttributes
import com.splunk.rum.integration.agent.api.extension.splunkRumId
import com.splunk.rum.integration.navigation.extension.navigation
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

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
        viewBinding.trackException.setOnClickListener(onClickListener)
        viewBinding.trackExceptionWithAttributes.setOnClickListener(onClickListener)
        viewBinding.addGlobalAttribute.setOnClickListener(onClickListener)
        viewBinding.removeGlobalAttribute.setOnClickListener(onClickListener)
        viewBinding.getGlobalAttribute.setOnClickListener(onClickListener)
        viewBinding.setAllGlobalAttributes.setOnClickListener(onClickListener)
        viewBinding.removeAllGlobalAttributes.setOnClickListener(onClickListener)
        viewBinding.getAllGlobalAttributes.setOnClickListener(onClickListener)
        viewBinding.crashReportsIllegal.splunkRumId = "illegalButton"

        SplunkRum.instance.navigation.track("Menu")
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
                SplunkRum.instance.customTracking.trackCustomEvent("TestEvent", testAttributes)
                showDoneToast("Track Custom Event, Done!")
            }
            viewBinding.trackWorkflow.id -> {
                val workflowSpan = SplunkRum.instance.customTracking.trackWorkflow("Test Workflow")
                workflowSpan?.setAttribute("workflow.start.time", System.currentTimeMillis())
                // Simulate some processing time
                Thread.sleep(125)
                workflowSpan?.setAttribute("workflow.end.time", System.currentTimeMillis())
                workflowSpan?.end()
                showDoneToast("Track Workflow, Done!")
            }
            viewBinding.trackException.id -> {
                val e = Exception("Custom Exception To Be Tracked");
                e.stackTrace = arrayOf(
                    StackTraceElement("android.fake.Crash", "crashMe", "NotARealFile.kt", 12),
                    StackTraceElement("android.fake.Class", "foo", "NotARealFile.kt", 34),
                    StackTraceElement("android.fake.Main", "main", "NotARealFile.kt", 56)
                )
                SplunkRum.instance.customTracking.trackException(e)
                showDoneToast("Track Exception, Done!")
            }
            viewBinding.trackExceptionWithAttributes.id -> {
                val e = Exception("Custom Exception (with attributes) To Be Tracked");
                e.stackTrace = arrayOf(
                    StackTraceElement("android.fake.Crash", "crashMe", "NotARealFile.kt", 12),
                    StackTraceElement("android.fake.Class", "foo", "NotARealFile.kt", 34),
                    StackTraceElement("android.fake.Main", "main", "NotARealFile.kt", 56)
                )
                val testAttributes = Attributes.builder()
                    .put("attribute.one", "value1")
                    .put("attribute.two", "12345")
                    .build()
                SplunkRum.instance.customTracking.trackException(e, testAttributes)
                showDoneToast("Track Exception with Attributes, Done!")
            }
            viewBinding.addGlobalAttribute.id -> {
                SplunkRum.instance.globalAttributes["globalAttributeKey"] = "globalAttributeVal"
            }
            viewBinding.removeGlobalAttribute.id -> {
                SplunkRum.instance.globalAttributes.remove("globalAttributeKey")
            }
            viewBinding.getGlobalAttribute.id -> {
                AlertDialog.Builder(context)
                    .setTitle("Key: globalAttributeKey")
                    .setMessage("Val: " + SplunkRum.instance.globalAttributes["globalAttributeKey"])
                    .setPositiveButton("OK", null)
                    .show()
            }
            viewBinding.setAllGlobalAttributes.id -> {
                val globalAttributes = Attributes.of(
                    AttributeKey.stringKey("globAttrKey1"), "12345",
                    AttributeKey.booleanKey("globAttrKey2"), true,
                    AttributeKey.doubleKey("globAttrKey3"), 1200.50,
                    AttributeKey.longKey("globAttrKey4"), 30L,
                    AttributeKey.stringKey("globAttrKey5"), "US"
                )
                SplunkRum.instance.globalAttributes.setAll(globalAttributes)
            }
            viewBinding.removeAllGlobalAttributes.id -> {
                SplunkRum.instance.globalAttributes.removeAll()
            }
            viewBinding.getAllGlobalAttributes.id -> {
                AlertDialog.Builder(context)
                    .setTitle("All Global Attributes")
                    .setMessage("All global attributes: " + SplunkRum.instance.globalAttributes.getAll().toString())
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showDoneToast(message: String) {
        runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
