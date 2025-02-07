package com.splunk.rum.integration.agent.api.extension

import android.view.View
import com.cisco.android.common.logger.Logger
import com.cisco.android.common.utils.extensions.ciscoId as internalCiscoId

private const val TAG = "ViewExt"

private val idRegex = "^[a-zA-Z][a-zA-Z0-9_\\.\\-,]{0,199}\$".toRegex() // https://regex101.com/r/r7RYao/1

var View.splunkRumId: String?
    get() = internalCiscoId
    set(value) {
        if (value != null && !idRegex.matches(value)) {
            Logger.w(TAG, "View.splunkRumId - invalid value")
            return
        }

        internalCiscoId = value
    }
