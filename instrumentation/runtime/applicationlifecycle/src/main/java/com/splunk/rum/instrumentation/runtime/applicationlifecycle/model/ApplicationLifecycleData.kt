package com.splunk.rum.instrumentation.runtime.applicationlifecycle.model

data class ApplicationLifecycleData(val startTimestamp: Long, val appState: AppState)