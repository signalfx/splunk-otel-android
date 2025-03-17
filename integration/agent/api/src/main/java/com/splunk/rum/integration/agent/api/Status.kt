package com.splunk.rum.integration.agent.api

sealed interface Status {
    object Running: Status
    data class NotRunning internal constructor(val cause: Cause): Status {
        enum class Cause {
            SampledOut,
            NotInstalled,
        }
    }
}