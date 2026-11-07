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

package com.splunk.rum.integration.agent.api.session

import com.splunk.rum.integration.agent.internal.session.NoOpSplunkSessionManager
import com.splunk.rum.integration.agent.internal.user.IUserManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock

class SessionWithoutIdTest {

    private val manager = NoOpSplunkSessionManager
    private val configuration = SessionConfiguration(samplingRate = 0.8)

    @Test
    fun `state keeps settings but has no session values`() {
        val state = SessionState(configuration, manager)

        assertNull(state.id)
        assertNull(state.start)
        assertNull(state.lastActivity)
        assertEquals(0.8, state.samplingRate, 0.0)
    }

    @Test
    fun `metadata is absent without a session`() {
        val session = Session(manager, mock(IUserManager::class.java), configuration)

        assertNull(session.metadata)
    }
}
