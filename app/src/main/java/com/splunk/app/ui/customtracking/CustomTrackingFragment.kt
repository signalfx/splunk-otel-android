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

package com.splunk.app.ui.customtracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.splunk.app.R
import com.splunk.app.databinding.FragmentCustomTrackingBinding
import com.splunk.app.ui.BaseFragment
import com.splunk.app.util.ApiVariant
import com.splunk.app.util.CommonUtils
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import com.splunk.rum.integration.customtracking.extension.customTracking
import com.splunk.rum.integration.navigation.extension.navigation

class CustomTrackingFragment : BaseFragment<FragmentCustomTrackingBinding>() {

    override val titleRes: Int = R.string.customtracking_title
    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCustomTrackingBinding
        get() = FragmentCustomTrackingBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Latest APIs
        viewBinding.trackCustomEvent.setOnClickListener(onClickListener)
        viewBinding.trackWorkflow.setOnClickListener(onClickListener)
        viewBinding.trackException.setOnClickListener(onClickListener)
        viewBinding.trackExceptionWithAttributes.setOnClickListener(onClickListener)

        // Legacy APIs
        viewBinding.trackCustomEventLegacy.setOnClickListener(onClickListener)
        viewBinding.trackWorkflowLegacy.setOnClickListener(onClickListener)
        viewBinding.trackExceptionLegacy.setOnClickListener(onClickListener)
        viewBinding.trackExceptionWithAttributesLegacy.setOnClickListener(onClickListener)

        SplunkRum.instance.navigation.track("CustomTracking")
    }

    private val onClickListener = View.OnClickListener {
        when (it.id) {
            // Latest APIs
            viewBinding.trackCustomEvent.id -> {
                SplunkRum.instance.customTracking.trackCustomEvent("TestEvent", getTestAttributes())
                CommonUtils.showDoneToast(context, "Track Custom Event")
            }

            viewBinding.trackWorkflow.id -> {
                simulateWorkflow(ApiVariant.NEXTGEN)
                CommonUtils.showDoneToast(context, "Track Workflow")
            }

            viewBinding.trackException.id -> {
                SplunkRum.instance.customTracking.trackException(getTestException("Custom Exception To Be Tracked"))
                CommonUtils.showDoneToast(context, "Track Exception")
            }

            viewBinding.trackExceptionWithAttributes.id -> {
                SplunkRum.instance.customTracking.trackException(
                    getTestException("Custom Exception (with attributes) To Be Tracked"),
                    getTestAttributes()
                )

                CommonUtils.showDoneToast(context, "Track Exception with Attributes")
            }

            // Legacy APIs
            viewBinding.trackCustomEventLegacy.id -> {
                SplunkRum.instance.addRumEvent("TestEvent", getTestAttributes())
                CommonUtils.showDoneToast(context, "Track Custom Event (Legacy)")
            }

            viewBinding.trackWorkflowLegacy.id -> {
                simulateWorkflow(ApiVariant.LEGACY)
                CommonUtils.showDoneToast(context, "Track Workflow (Legacy)")
            }

            viewBinding.trackExceptionLegacy.id -> {
                SplunkRum.instance.addRumException(getTestException("Custom Exception To Be Tracked"))
                CommonUtils.showDoneToast(context, "Track Exception (Legacy)")
            }

            viewBinding.trackExceptionWithAttributesLegacy.id -> {
                SplunkRum.instance.addRumException(
                    getTestException("Custom Exception (with attributes) To Be Tracked"),
                    getTestAttributes()
                )

                CommonUtils.showDoneToast(context, "Track Exception with Attributes (Legacy)")
            }
        }
    }

    private fun simulateWorkflow(implementationType: ApiVariant) {
        val workflowSpan = when (implementationType) {
            ApiVariant.NEXTGEN -> SplunkRum.instance.customTracking.trackWorkflow("Test Workflow")
            ApiVariant.LEGACY -> SplunkRum.instance.startWorkflow("Test Workflow")
        }

        workflowSpan?.apply {
            val startTime = System.currentTimeMillis()
            setAttribute("workflow.start.time", startTime)
            setAttribute("workflow.end.time", startTime + 125) // Added 125ms processing time in end time.
            end()
        }
    }

    private fun getTestAttributes(): MutableAttributes = MutableAttributes().also { attributes ->
        attributes["attribute.one"] = "value1"
        attributes["attribute.two"] = "12345"
    }

    private fun getTestException(message: String): Exception {
        val e = Exception(message)
        e.stackTrace = arrayOf(
            StackTraceElement("android.fake.Crash", "crashMe", "NotARealFile.kt", 12),
            StackTraceElement("android.fake.Class", "foo", "NotARealFile.kt", 34),
            StackTraceElement("android.fake.Main", "main", "NotARealFile.kt", 56)
        )
        return e
    }
}
