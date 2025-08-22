package com.splunk.rum.instrumentation.runtime.applicationlifecycle.model

enum class AppState(private val stateName: String) {
    CREATED("created"),
    FOREGROUND("foreground"),
    BACKGROUND("background");

    override fun toString(): String = stateName
}
