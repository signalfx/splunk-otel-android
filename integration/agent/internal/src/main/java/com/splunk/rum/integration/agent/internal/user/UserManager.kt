package com.splunk.rum.integration.agent.internal.user

import com.cisco.android.common.id.NanoId

interface IUserManager {
    var trackingMode: InternalUserTrackingMode
    val userId: String?
}

class UserManager : IUserManager {
    override var trackingMode: InternalUserTrackingMode = InternalUserTrackingMode.NoTracking
        set(value) {
            userId = when (value) {
                InternalUserTrackingMode.NoTracking -> null
                InternalUserTrackingMode.AnonymousTracking -> NanoId.generate()
            }
            field = value
        }

    override var userId: String? = null
        private set
}

object NoOpUserManager : IUserManager {
    override var trackingMode: InternalUserTrackingMode = InternalUserTrackingMode.NoTracking
    override val userId: String? = null
}

enum class InternalUserTrackingMode {
    NoTracking, AnonymousTracking
}