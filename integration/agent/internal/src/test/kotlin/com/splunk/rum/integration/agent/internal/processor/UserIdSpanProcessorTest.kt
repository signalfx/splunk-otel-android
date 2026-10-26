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

package com.splunk.rum.integration.agent.internal.processor

import com.splunk.rum.integration.agent.internal.RumConstants
import com.splunk.rum.integration.agent.internal.user.IUserManager
import com.splunk.rum.integration.agent.internal.user.InternalUserTrackingMode
import com.splunk.rum.integration.agent.internal.user.PENDING_USER_ID
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class UserIdSpanProcessorTest {

    @Test
    fun `main thread sets the pending user id without waiting for the user id`() {
        val span = mock(ReadWriteSpan::class.java)
        val userManager = FakeUserManager(telemetryUserId = PENDING_USER_ID, waitedUserId = null)

        UserIdSpanProcessor(userManager, isMainThread = { true }).onStart(Context.root(), span)

        verify(span).setAttribute(RumConstants.USER_ID_KEY, PENDING_USER_ID)
    }

    @Test
    fun `other threads wait for the user id`() {
        val span = mock(ReadWriteSpan::class.java)
        val userManager = FakeUserManager(telemetryUserId = null, waitedUserId = "user-id")

        UserIdSpanProcessor(userManager, isMainThread = { false }).onStart(Context.root(), span)

        verify(span).setAttribute(RumConstants.USER_ID_KEY, "user-id")
    }

    /** [userId] is unavailable when [waitedUserId] is null, so a test fails if the processor waits for it. */
    private class FakeUserManager(override val telemetryUserId: String?, private val waitedUserId: String?) :
        IUserManager {
        override var trackingMode: InternalUserTrackingMode = InternalUserTrackingMode.ANONYMOUS_TRACKING

        override val userId: String?
            get() = checkNotNull(waitedUserId) { "The processor must not wait for the user id" }
    }
}
