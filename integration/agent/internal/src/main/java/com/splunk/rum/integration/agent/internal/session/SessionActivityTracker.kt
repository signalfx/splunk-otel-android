/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.agent.internal.session

import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks latest activity in session. We consider activity as new span or log creation.
 * Session start is not considered as activity.
 */
class SessionActivityTracker : ISessionActivityTracker {

    private val lastActivityTimestamp = AtomicLong(NO_ACTIVITY)

    override val sessionLastActivity: Long?
        get() = lastActivityTimestamp.get().takeIf { it != NO_ACTIVITY }

    override fun trackActivity() {
        lastActivityTimestamp.set(System.currentTimeMillis())
    }

    override fun reset() {
        lastActivityTimestamp.set(NO_ACTIVITY)
    }

    private companion object {
        const val NO_ACTIVITY = -1L
    }
}

interface ISessionActivityTracker {
    val sessionLastActivity: Long?

    fun trackActivity()
    fun reset()
}

object NoOpSessionActivityTracker : ISessionActivityTracker {
    override val sessionLastActivity: Long? = null

    override fun trackActivity() = Unit

    override fun reset() = Unit
}
