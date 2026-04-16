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

import com.splunk.rum.common.storage.IAgentStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class UserManagerTest {

    @Test
    fun `initial user id is null when tracking disabled`() {
        val (storage, state) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.NO_TRACKING, storage)

        assertEquals(InternalUserTrackingMode.NO_TRACKING, userManager.trackingMode)
        assertNull(userManager.userId)
        assertNull(state.anonymousUserId)
    }

    @Test
    fun `initial user id is loaded from storage when anonymous tracking enabled`() {
        val (storage, _) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage)

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertEquals("persisted-user-id", userManager.userId)
    }

    @Test
    fun `initial user id is generated and persisted when anonymous tracking enabled and missing`() {
        val (storage, state) = storageMock()
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage)

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertNotNull(userManager.userId)
        assertEquals(state.anonymousUserId, userManager.userId)
    }

    @Test
    fun `switching to no tracking clears user id`() {
        val (storage, state) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage)
        assertNotNull(userManager.userId)

        userManager.trackingMode = InternalUserTrackingMode.NO_TRACKING

        assertEquals(InternalUserTrackingMode.NO_TRACKING, userManager.trackingMode)
        assertNull(userManager.userId)
        assertNull(state.anonymousUserId)
    }

    @Test
    fun `switching between anonymous tracking preserves user id`() {
        val (storage, _) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage)
        val initialUserId = userManager.userId

        userManager.trackingMode = InternalUserTrackingMode.ANONYMOUS_TRACKING

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertEquals(initialUserId, userManager.userId)
    }

    @Test
    fun `disabling and re enabling anonymous tracking generates new id`() {
        val (storage, _) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage)
        val initialUserId = userManager.userId

        userManager.trackingMode = InternalUserTrackingMode.NO_TRACKING
        assertNull(userManager.userId)

        userManager.trackingMode = InternalUserTrackingMode.ANONYMOUS_TRACKING

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertNotNull(userManager.userId)
        assertNotEquals(initialUserId, userManager.userId)
    }

    @Test
    fun `user id always reflects latest persisted anonymous user id`() {
        val (storage, state) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage)

        state.anonymousUserId = "updated-user-id"

        assertEquals("updated-user-id", userManager.userId)
    }

    data class StorageState(var anonymousUserId: String? = null)

    private fun storageMock(anonymousUserId: String? = null): Pair<IAgentStorage, StorageState> {
        val state = StorageState(anonymousUserId = anonymousUserId)
        val storage = mock(IAgentStorage::class.java)

        `when`(storage.readAnonymousUserId()).thenAnswer { state.anonymousUserId }
        doAnswer { invocation ->
            state.anonymousUserId = invocation.getArgument(0)
            null
        }.`when`(storage).writeAnonymousUserId(anyString())
        doAnswer {
            state.anonymousUserId = null
            null
        }.`when`(storage).deleteAnonymousUserId()

        return storage to state
    }
}
