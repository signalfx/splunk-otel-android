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

package com.splunk.rum.integration.agent.internal.attributes

import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.agent.internal.processor.SplunkInternalGlobalAttributeSpanProcessor

object ScreenNameTracker : IScreenNameTracker {
    override var lastScreenName: String? = null
        private set

    override var screenName: String = RumConstants.DEFAULT_SCREEN_NAME
        set(value) {
            if (field != value && field != RumConstants.DEFAULT_SCREEN_NAME) {
                lastScreenName = field
            }
            field = value
            SplunkInternalGlobalAttributeSpanProcessor.attributes[RumConstants.SCREEN_NAME_KEY] = value
        }
        get() = field
}
