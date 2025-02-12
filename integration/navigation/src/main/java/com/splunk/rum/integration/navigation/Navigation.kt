package com.splunk.rum.integration.navigation

object Navigation {

    internal var listener: Listener? = null

    val preferences = Preferences()

    fun track(screenName: String) {
        listener?.onScreenNameChanged(screenName)
    }

    class Preferences {
        var isFragmentTrackingEnabled = false // TODO implementation
        var isActivityTrackingEnabled = false // TODO implementation
    }

    internal interface Listener {
        fun onScreenNameChanged(screenName: String)
    }
}