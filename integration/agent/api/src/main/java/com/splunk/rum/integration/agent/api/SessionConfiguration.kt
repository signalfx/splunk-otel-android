package com.splunk.rum.integration.agent.api

import com.cisco.android.common.logger.Logger

data class SessionConfiguration(
    val samplingRate: Double = 1.0
) {
    init {
        if (samplingRate !in 0.0..1.0) {
            Logger.e("SessionConfiguration", "samplingRate = $samplingRate is not in allowed range 0.0 <= sampling rate <= 1.0")
        }
    }
}