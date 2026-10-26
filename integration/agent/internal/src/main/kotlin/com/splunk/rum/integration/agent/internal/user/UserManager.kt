/*
 * Copyright 2025 Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum.integration.agent.internal.user

import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.common.logger.Logger
import com.splunk.rum.common.utils.thread.NamedThreadFactory
import com.splunk.rum.integration.agent.internal.RumConstants
import com.splunk.rum.integration.agent.internal.id.UserId
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

interface IUserManager {
    var trackingMode: InternalUserTrackingMode

    /** May wait while the user ID stored by an earlier launch is read from storage. */
    val userId: String?

    /** Never waits. Returns [PENDING_USER_ID] instead of a user ID that is still being read. */
    val telemetryUserId: String?
        get() = userId

    /** The user ID that [PENDING_USER_ID] stands for. Returns null instead of waiting when [wait] is false. */
    fun pendingUserId(wait: Boolean): String? = null
}

/** Value of the user ID span attribute until the user ID is read from storage. Never exported. */
internal const val PENDING_USER_ID = "splunk.internal.pendingUserId"

/**
 * Replaces [PENDING_USER_ID] with the user ID it stands for. Without a value, the attribute is removed when
 * [wait] is true and kept when it is false.
 */
fun IUserManager.resolvePendingUserId(attributes: Attributes, wait: Boolean): Attributes {
    if (attributes.get(RumConstants.USER_ID_KEY) != PENDING_USER_ID) {
        return attributes
    }

    val userId = pendingUserId(wait)

    return when {
        userId != null -> attributes.toBuilder().put(RumConstants.USER_ID_KEY, userId).build()
        wait -> attributes.toBuilder().remove(RumConstants.USER_ID_KEY).build()
        else -> attributes
    }
}

/**
 * Storage operations run on [storageExecutor] in the requested order, so the user ID stored by an earlier
 * launch is read before a later tracking mode change deletes it.
 */
class UserManager(
    userTrackingMode: InternalUserTrackingMode,
    private val agentStorage: IAgentStorage,
    private val storageExecutor: Executor = newStorageExecutor()
) : IUserManager {

    private val initialUserId = FutureTask(::readOrCreateUserId)

    /** Null while tracking is disabled. Only [initialUserId] can still be pending. */
    @Volatile
    private var currentUserId: Future<String>? = null

    init {
        if (userTrackingMode == InternalUserTrackingMode.ANONYMOUS_TRACKING) {
            currentUserId = initialUserId
            storageExecutor.execute(initialUserId)
        } else {
            updateAnonymousUserIdForTrackingMode(userTrackingMode)
        }
    }

    override val userId: String?
        get() = currentUserId?.await()

    override val telemetryUserId: String?
        get() = currentUserId?.let { if (it.isDone) it.await() else PENDING_USER_ID }

    override fun pendingUserId(wait: Boolean): String? =
        if (wait || initialUserId.isDone) initialUserId.await() else null

    @set:Synchronized
    override var trackingMode: InternalUserTrackingMode = userTrackingMode
        set(value) {
            updateAnonymousUserIdForTrackingMode(value)
            field = value
        }

    private fun updateAnonymousUserIdForTrackingMode(trackingMode: InternalUserTrackingMode) {
        when (trackingMode) {
            InternalUserTrackingMode.NO_TRACKING -> {
                Logger.d(TAG) { "User tracking disabled, deleting anonymous user id." }
                currentUserId = null
                storageExecutor.execute { agentStorage.deleteAnonymousUserId() }
            }
            InternalUserTrackingMode.ANONYMOUS_TRACKING -> if (currentUserId == null) {
                val newUserId = UserId.generate()
                currentUserId = FutureTask { newUserId }.apply { run() }
                storageExecutor.execute { agentStorage.writeAnonymousUserId(newUserId) }
                Logger.d(TAG) { "User tracking enabled. Generated new anonymous user id: $newUserId" }
            }
        }
    }

    private fun readOrCreateUserId(): String {
        val existingUserId = agentStorage.readAnonymousUserId()
        if (existingUserId != null) {
            Logger.d(TAG) { "User tracking enabled. Existing anonymous user id: $existingUserId" }
            return existingUserId
        }

        val newUserId = UserId.generate()
        agentStorage.writeAnonymousUserId(newUserId)
        Logger.d(TAG) { "User tracking enabled. Generated new anonymous user id: $newUserId" }
        return newUserId
    }

    companion object {
        private const val TAG = "UserManager"
        private const val STORAGE_THREAD_KEEP_ALIVE_SECONDS = 5L

        /** Runs tasks in submission order on a single thread that ends when idle. */
        private fun newStorageExecutor(): Executor = ThreadPoolExecutor(
            0,
            1,
            STORAGE_THREAD_KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            NamedThreadFactory("SplunkUserStorage")
        )
    }
}

/** Returns null when the value could not be read or the waiting thread was interrupted. */
private fun Future<String>.await(): String? = try {
    get()
} catch (_: ExecutionException) {
    null
} catch (_: InterruptedException) {
    Thread.currentThread().interrupt()
    null
}

object NoOpUserManager : IUserManager {
    override var trackingMode: InternalUserTrackingMode = InternalUserTrackingMode.NO_TRACKING
    override val userId: String? = null
}

enum class InternalUserTrackingMode {
    NO_TRACKING,
    ANONYMOUS_TRACKING
}
