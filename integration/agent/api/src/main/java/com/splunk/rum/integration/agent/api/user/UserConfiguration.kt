package com.splunk.rum.integration.agent.api.user

data class UserConfiguration(
    val trackingMode: UserTrackingMode = UserTrackingMode.NoTracking
)

enum class UserTrackingMode {
    NoTracking, AnonymousTracking
}