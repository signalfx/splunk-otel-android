package com.splunk.rum.integration.agent.api.user

import com.splunk.rum.integration.agent.internal.user.IUserManager
import com.splunk.rum.integration.agent.internal.user.UserManager
import com.splunk.rum.integration.agent.internal.user.InternalUserTrackingMode

class User internal constructor(
    userManager: IUserManager
) {
    val state: State = State(userManager)
    val preferences: Preferences = Preferences(userManager)
}

class State internal constructor(private val userManager: IUserManager) {
    val trackingMode: UserTrackingMode
        get() = userManager.trackingMode.toPublic()
}

class Preferences internal constructor(private val userManager: IUserManager) {
    var trackingMode: UserTrackingMode? = null
        set(value) {
            field = value
            value?.let {
                userManager.trackingMode = it.toInternal()
            }
        }
}

private fun UserTrackingMode.toInternal(): InternalUserTrackingMode = when (this) {
    UserTrackingMode.NoTracking -> InternalUserTrackingMode.NoTracking
    UserTrackingMode.AnonymousTracking -> InternalUserTrackingMode.AnonymousTracking
}

private fun InternalUserTrackingMode.toPublic(): UserTrackingMode = when (this) {
    InternalUserTrackingMode.NoTracking -> UserTrackingMode.NoTracking
    InternalUserTrackingMode.AnonymousTracking -> UserTrackingMode.AnonymousTracking
}
