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
import com.splunk.app.extension.showToast
import com.splunk.app.ui.BaseFragment
import com.splunk.app.util.ApiVariant
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import com.splunk.rum.integration.customtracking.extension.customTracking

class CustomTrackingFragment : BaseFragment<FragmentCustomTrackingBinding>() {

    override val navigationName = "CustomTracking"

    override val titleRes: Int = R.string.customtracking_title

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCustomTrackingBinding
        get() = FragmentCustomTrackingBinding::inflate

    private val testAttributes = MutableAttributes().apply {
        this["user.id"] = "user-123"
        this["user.email"] = "test.user@example.com"
        this["screen.name"] = "CustomTracking"
        this["experiment.group"] = "A"
    }

    private val testException: Exception
        get() = Exception("TestException").apply {
            stackTrace = arrayOf(
                StackTraceElement("android.fake.Crash", "crashMe", "NotARealFile.kt", 12),
                StackTraceElement("android.fake.Class", "foo", "NotARealFile.kt", 34),
                StackTraceElement("android.fake.Main", "main", "NotARealFile.kt", 56)
            )
        }

    private val splunkRum = SplunkRum.instance
    private val customTracking = splunkRum.customTracking

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
    }

    private val onClickListener = View.OnClickListener { view ->
        when (view.id) {
            // Latest APIs
            viewBinding.trackCustomEvent.id -> trackEvent(ApiVariant.LATEST)
            viewBinding.trackWorkflow.id -> trackWorkflow(ApiVariant.LATEST)
            viewBinding.trackException.id -> trackException(ApiVariant.LATEST)
            viewBinding.trackExceptionWithAttributes.id -> trackException(ApiVariant.LATEST, testAttributes)

            // Legacy APIs
            viewBinding.trackCustomEventLegacy.id -> trackEvent(ApiVariant.LEGACY)
            viewBinding.trackWorkflowLegacy.id -> trackWorkflow(ApiVariant.LEGACY)
            viewBinding.trackExceptionLegacy.id -> trackException(ApiVariant.LEGACY)
            viewBinding.trackExceptionWithAttributesLegacy.id -> trackException(ApiVariant.LEGACY, testAttributes)
        }
    }

    private fun trackEvent(variant: ApiVariant) {
        when (variant) {
            ApiVariant.LATEST -> customTracking.trackCustomEvent("TestEvent", testAttributes)
            ApiVariant.LEGACY -> splunkRum.addRumEvent("TestEvent", testAttributes)
        }

        context?.showToast(R.string.toast_custom_event)
    }

    private fun trackWorkflow(variant: ApiVariant) {
        val workflowSpan = when (variant) {
            ApiVariant.LATEST -> customTracking.trackWorkflow("TestWorkflow")
            ApiVariant.LEGACY -> splunkRum.startWorkflow("TestWorkflow")
        }

        workflowSpan?.apply {
            val startTime = System.currentTimeMillis()
            setAttribute("workflow.start.time", startTime)
            setAttribute("workflow.end.time", startTime + 125)
            end()
        }

        context?.showToast(R.string.toast_workflow)
    }

    private fun trackException(variant: ApiVariant, attributes: MutableAttributes? = null) {
        when (variant) {
            ApiVariant.LATEST -> {
                if (attributes == null) {
                    customTracking.trackException(testException)
                } else {
                    customTracking.trackException(testException, attributes)
                }
            }
            ApiVariant.LEGACY -> {
                if (attributes == null) {
                    splunkRum.addRumException(testException)
                } else {
                    splunkRum.addRumException(testException, attributes)
                }
            }
        }

        context?.showToast(R.string.toast_exception)
    }
}
