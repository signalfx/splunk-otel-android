package com.splunk.rum.integration.agent.internal.attributes

import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.agent.internal.processor.SplunkInternalGlobalAttributeSpanProcessor

object ScreenNameTracker {
    var lastScreenName: String? = null
        private set

    var screenName: String = RumConstants.DEFAULT_SCREEN_NAME
        set(value) {
            if (field != value && field != RumConstants.DEFAULT_SCREEN_NAME) {
                lastScreenName = field
                SplunkInternalGlobalAttributeSpanProcessor.attributes[RumConstants.LAST_SCREEN_NAME_KEY] = field
            }
            field = value
            SplunkInternalGlobalAttributeSpanProcessor.attributes[RumConstants.SCREEN_NAME_KEY] = value
        }
        get() = field
}
