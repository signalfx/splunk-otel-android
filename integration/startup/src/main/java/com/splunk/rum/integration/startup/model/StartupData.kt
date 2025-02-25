package com.splunk.rum.integration.startup.model

internal data class StartupData(
    val startTimestamp: Long,
    val endTimestamp: Long,
    val name: String
)