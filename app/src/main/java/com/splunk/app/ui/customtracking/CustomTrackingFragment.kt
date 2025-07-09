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
import com.splunk.app.extension.showDoneToast
import com.splunk.app.ui.BaseFragment
import com.splunk.app.util.ApiVariant
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import com.splunk.rum.integration.customtracking.extension.customTracking

/**
 * Fragment for demonstrating usage of the Splunk RUM Custom Tracking API.
 *
 * This fragment provides buttons to test both the latest and legacy APIs for:
 * - Tracking custom events
 * - Tracking workflows (spans)
 * - Reporting exceptions (with and without attributes)
 *
 */
class CustomTrackingFragment : BaseFragment<FragmentCustomTrackingBinding>() {

    override val titleRes: Int = R.string.custom_tracking_title

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentCustomTrackingBinding
        get() = FragmentCustomTrackingBinding::inflate

    /**
     * A set of test attributes used when sending events, spans, and exceptions.
     */
    private val testAttributes = MutableAttributes().apply {
        this["user.id"] = "user-123"
        this["user.email"] = "test.user@example.com"
        this["screen.name"] = "CustomTracking"
        this["experiment.group"] = "A"
    }

    /**
     * A simulated test exception with a synthetic stack trace.
     */
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

        with(viewBinding) {
            // Latest APIs
            trackCustomEvent.setOnClickListener { trackEvent(ApiVariant.LATEST) }
            trackWorkflow.setOnClickListener { trackWorkflow(ApiVariant.LATEST) }
            trackException.setOnClickListener { trackException(ApiVariant.LATEST) }
            trackExceptionWithAttributes.setOnClickListener { trackException(ApiVariant.LATEST, testAttributes) }

            // Legacy APIs
            trackCustomEventLegacy.setOnClickListener { trackEvent(ApiVariant.LEGACY) }
            trackWorkflowLegacy.setOnClickListener { trackWorkflow(ApiVariant.LEGACY) }
            trackExceptionLegacy.setOnClickListener { trackException(ApiVariant.LEGACY) }
            trackExceptionWithAttributesLegacy.setOnClickListener { trackException(ApiVariant.LEGACY, testAttributes) }
        }
    }

    /**
     * Tracks a custom event with optional attributes using the specified API variant.
     *
     * @param variant The API variant to use: [ApiVariant.LATEST] or [ApiVariant.LEGACY].
     */
    private fun trackEvent(variant: ApiVariant) {
        when (variant) {
            ApiVariant.LATEST -> customTracking.trackCustomEvent("TestEvent", testAttributes)
            ApiVariant.LEGACY -> splunkRum.addRumEvent("TestEvent", testAttributes)
        }

        context?.showDoneToast(R.string.track_custom_event, variant)
    }

    /**
     * Tracks a workflow span and ends it immediately.
     *
     * @param variant The API variant to use for tracking the workflow.
     */
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

        context?.showDoneToast(R.string.track_workflow, variant)
    }

    /**
     * Tracks an exception with optional attributes using the selected API variant.
     *
     * @param variant The API variant to use.
     * @param attributes Optional set of attributes to include with the exception.
     */
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

        context?.showDoneToast(R.string.track_exception, variant)
    }
}
