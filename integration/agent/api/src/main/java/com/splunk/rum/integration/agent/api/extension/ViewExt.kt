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

package com.splunk.rum.integration.agent.api.extension

import android.view.View
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.extensions.ciscoId as internalCiscoId

private const val TAG = "ViewExt"

private val idRegex = "^[a-zA-Z][a-zA-Z0-9_\\.\\-,]{0,199}\$".toRegex() // https://regex101.com/r/r7RYao/1

/**
 * UI element identification. Useful for wireframe and interactions.
 */
var View.splunkRumId: String?
    get() = internalCiscoId
    set(value) {
        if (value != null && !idRegex.matches(value)) {
            Logger.w(TAG, "View.splunkRumId - invalid value")
            return
        }

        internalCiscoId = value
    }
