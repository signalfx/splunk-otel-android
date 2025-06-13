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

package com.splunk.rum.integration.agent.internal.processor

import android.app.Application
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.utils.extensions.applicationId
import com.splunk.rum.utils.extensions.splunkBuildId
import com.splunk.rum.utils.extensions.versionCode
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class ErrorIdentifierAttributesSpanProcessor(application: Application) : SpanProcessor {

    private var applicationId: String? = application.applicationId
    private var versionCode: String? = application.versionCode
    private var splunkBuildId: String? = application.splunkBuildId

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        if (span.getAttribute(RumConstants.COMPONENT_KEY) == RumConstants.COMPONENT_ERROR ||
            span.getAttribute(RumConstants.COMPONENT_KEY) == RumConstants.COMPONENT_CRASH
        ) {
            applicationId?.let {
                span.setAttribute(RumConstants.APPLICATION_ID_KEY, it)
            }
            versionCode?.let {
                span.setAttribute(RumConstants.APP_VERSION_CODE_KEY, it)
            }
            splunkBuildId?.let {
                span.setAttribute(RumConstants.SPLUNK_BUILD_ID, it)
            }
        }
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) = Unit

    override fun isEndRequired(): Boolean = false
}
