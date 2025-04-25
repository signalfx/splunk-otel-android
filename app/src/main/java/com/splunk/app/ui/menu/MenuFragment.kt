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
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.agent.api.attributes.MutableAttributes
import com.splunk.rum.integration.agent.api.extension.splunkRumId
import com.splunk.rum.integration.customtracking.extension.customTracking
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

        viewBinding.setStringAttribute.setOnClickListener(onClickListener)
        viewBinding.setLongAttribute.setOnClickListener(onClickListener)
        viewBinding.setDoubleAttribute.setOnClickListener(onClickListener)
        viewBinding.setBooleanAttribute.setOnClickListener(onClickListener)
        viewBinding.setGenericAttribute.setOnClickListener(onClickListener)
        viewBinding.removeStringAttribute.setOnClickListener(onClickListener)
        viewBinding.removeGenericAttribute.setOnClickListener(onClickListener)
        viewBinding.getStringAttribute.setOnClickListener(onClickListener)
        viewBinding.getGenericAttribute.setOnClickListener(onClickListener)
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
                val testAttributes = MutableAttributes().also { attributes ->
                    attributes["attribute.one"] = "value1"
                    attributes["attribute.two"] = "12345"
                }

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
                val testAttributes = MutableAttributes().also { attributes ->
                    attributes["attribute.one"] = "value1"
                    attributes["attribute.two"] = "12345"
                }

                SplunkRum.instance.customTracking.trackException(e, testAttributes)
                showDoneToast("Track Exception with Attributes")
            }
            viewBinding.setStringAttribute.id -> {
                SplunkRum.instance.globalAttributes["stringKey"] = "String Value"
                showDoneToast("Set String Global Attribute")
            }
            viewBinding.setLongAttribute.id -> {
                SplunkRum.instance.globalAttributes["longKey"] = 12345L
                showDoneToast("Set Long Global Attribute")
            }
            viewBinding.setDoubleAttribute.id -> {
                SplunkRum.instance.globalAttributes["doubleKey"] = 123.45
                showDoneToast("Set Double Global Attribute")
            }
            viewBinding.setBooleanAttribute.id -> {
                SplunkRum.instance.globalAttributes["booleanKey"] = true
                showDoneToast("Set Boolean Global Attribute")
            }
            viewBinding.setGenericAttribute.id -> {
                val key = AttributeKey.stringKey("genericKey")
                SplunkRum.instance.globalAttributes[key] = "Generic Value"
                showDoneToast("Set Generic Global Attribute")
            }
            viewBinding.removeStringAttribute.id -> {
                SplunkRum.instance.globalAttributes.remove("stringKey")
                showDoneToast("Remove String Global Attribute")
            }
            viewBinding.removeGenericAttribute.id -> {
                val key = AttributeKey.stringKey("genericKey")
                SplunkRum.instance.globalAttributes.remove(key)
                showDoneToast("Remove Generic Global Attribute")
            }
            viewBinding.getStringAttribute.id -> {
                val value: String? = SplunkRum.instance.globalAttributes["stringKey"]
                AlertDialog.Builder(context)
                    .setTitle("Key: stringKey")
                    .setMessage("Value: $value")
                    .setPositiveButton("OK", null)
                    .show()
            }
            viewBinding.getGenericAttribute.id -> {
                val key = AttributeKey.stringKey("genericKey")
                val value = SplunkRum.instance.globalAttributes[key]
                AlertDialog.Builder(context)
                    .setTitle("Key: genericKey")
                    .setMessage("Value: $value")
                    .setPositiveButton("OK", null)
                    .show()
            }
            viewBinding.setAllGlobalAttributes.id -> {
                val globalAttributes = Attributes.of(
                    AttributeKey.stringKey("setAllString"), "String Value",
                    AttributeKey.booleanKey("setAllBoolean"), true,
                    AttributeKey.doubleKey("setAllDouble"), 456.78,
                    AttributeKey.longKey("setAllLong"), 9876L
                )
                SplunkRum.instance.globalAttributes.setAll(globalAttributes)
                showDoneToast("Set All Global Attributes")
            }
            viewBinding.removeAllGlobalAttributes.id -> {
                SplunkRum.instance.globalAttributes.removeAll()
                showDoneToast("Remove All Global Attributes")
            }
            viewBinding.getAllGlobalAttributes.id -> {
                val allAttributes = SplunkRum.instance.globalAttributes
                val attributesText = StringBuilder()
                allAttributes.forEach { key, value ->
                    attributesText.append("${key.key}: $value\n")
                }

                AlertDialog.Builder(context)
                    .setTitle("All Global Attributes")
                    .setMessage(attributesText.toString())
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