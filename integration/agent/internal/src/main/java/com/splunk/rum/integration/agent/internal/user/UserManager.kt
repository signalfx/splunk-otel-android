package com.splunk.rum.integration.agent.internal.user

import com.splunk.android.common.id.NanoId

interface IUserManager {
    var trackingMode: InternalUserTrackingMode
    val userId: String?
}

class UserManager(userTrackingMode: InternalUserTrackingMode) : IUserManager {

    override var userId: String? = userTrackingMode.initialUserId()
        private set

    override var trackingMode: InternalUserTrackingMode = userTrackingMode
        set(value) {
            userId = value.updatedUserId(currentTrackingMode = field, currentUserId = userId)
            field = value
        }
}

object NoOpUserManager : IUserManager {
    override var trackingMode: InternalUserTrackingMode = InternalUserTrackingMode.NO_TRACKING
    override val userId: String? = null
}

enum class InternalUserTrackingMode {
    NO_TRACKING,
    ANONYMOUS_TRACKING
}


private fun InternalUserTrackingMode.initialUserId(): String? =
    when (this) {
        InternalUserTrackingMode.NO_TRACKING -> null
        InternalUserTrackingMode.ANONYMOUS_TRACKING -> NanoId.generate()
    }

private fun InternalUserTrackingMode.updatedUserId(
    currentTrackingMode: InternalUserTrackingMode,
    currentUserId: String?
): String? {
    return when (this) {
        InternalUserTrackingMode.NO_TRACKING -> null
        InternalUserTrackingMode.ANONYMOUS_TRACKING -> {
            if (currentTrackingMode == InternalUserTrackingMode.ANONYMOUS_TRACKING && currentUserId != null) {
                currentUserId
            } else {
                NanoId.generate()
            }
        }
    }
}