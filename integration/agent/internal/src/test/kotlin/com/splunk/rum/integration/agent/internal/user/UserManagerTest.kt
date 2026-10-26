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
import com.splunk.rum.integration.agent.internal.RumConstants
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class UserManagerTest {

    private val directExecutor = Executor { it.run() }

    @Test
    fun `initial user id is null when tracking disabled`() {
        val (storage, state) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.NO_TRACKING, storage, directExecutor)

        assertEquals(InternalUserTrackingMode.NO_TRACKING, userManager.trackingMode)
        assertNull(userManager.userId)
        assertNull(userManager.telemetryUserId)
        assertNull(state.anonymousUserId)
    }

    @Test
    fun `initial user id is loaded from storage when anonymous tracking enabled`() {
        val (storage, _) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage, directExecutor)

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertEquals("persisted-user-id", userManager.userId)
        assertEquals("persisted-user-id", userManager.telemetryUserId)
    }

    @Test
    fun `initial user id is generated and persisted when anonymous tracking enabled and missing`() {
        val (storage, state) = storageMock()
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage, directExecutor)

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertNotNull(userManager.userId)
        assertEquals(state.anonymousUserId, userManager.userId)
    }

    @Test
    fun `switching to no tracking clears user id`() {
        val (storage, state) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage, directExecutor)
        assertNotNull(userManager.userId)

        userManager.trackingMode = InternalUserTrackingMode.NO_TRACKING

        assertEquals(InternalUserTrackingMode.NO_TRACKING, userManager.trackingMode)
        assertNull(userManager.userId)
        assertNull(userManager.telemetryUserId)
        assertNull(state.anonymousUserId)
    }

    @Test
    fun `switching between anonymous tracking preserves user id`() {
        val (storage, state) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage, directExecutor)
        val initialUserId = userManager.userId

        userManager.trackingMode = InternalUserTrackingMode.ANONYMOUS_TRACKING

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertEquals(initialUserId, userManager.userId)
        assertEquals(initialUserId, state.anonymousUserId)
    }

    @Test
    fun `disabling and re enabling anonymous tracking generates new id`() {
        val (storage, state) = storageMock(anonymousUserId = "persisted-user-id")
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage, directExecutor)
        val initialUserId = userManager.userId

        userManager.trackingMode = InternalUserTrackingMode.NO_TRACKING
        assertNull(userManager.userId)

        userManager.trackingMode = InternalUserTrackingMode.ANONYMOUS_TRACKING

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertNotNull(userManager.userId)
        assertNotEquals(initialUserId, userManager.userId)
        assertEquals(userManager.userId, state.anonymousUserId)
    }

    @Test
    fun `initial user id is not read on the calling thread`() {
        val (storage, _) = storageMock(anonymousUserId = "persisted-user-id")
        val executor = QueuedExecutor()
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage, executor)

        verify(storage, never()).readAnonymousUserId()
        assertEquals(PENDING_USER_ID, userManager.telemetryUserId)
        assertNull(userManager.pendingUserId(wait = false))

        executor.runQueuedTasks()

        assertEquals("persisted-user-id", userManager.telemetryUserId)
        assertEquals("persisted-user-id", userManager.pendingUserId(wait = false))
        assertEquals("persisted-user-id", userManager.userId)
    }

    @Test
    fun `user id waits until the initial user id is read`() {
        val (storage, _) = storageMock(anonymousUserId = "persisted-user-id")
        val executor = Executors.newSingleThreadExecutor()
        val releaseStorage = CountDownLatch(1)
        executor.execute { releaseStorage.await(5, TimeUnit.SECONDS) }
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage, executor)
        val userId = AtomicReference<String>()
        val readFinished = CountDownLatch(1)

        Thread {
            userId.set(userManager.userId)
            readFinished.countDown()
        }.start()

        assertFalse(readFinished.await(100, TimeUnit.MILLISECONDS))

        releaseStorage.countDown()

        assertTrue(readFinished.await(5, TimeUnit.SECONDS))
        assertEquals("persisted-user-id", userId.get())
    }

    @Test
    fun `tracking mode changes are applied after the initial user id is read`() {
        val (storage, state) = storageMock(anonymousUserId = "persisted-user-id")
        val executor = QueuedExecutor()
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage, executor)

        userManager.trackingMode = InternalUserTrackingMode.NO_TRACKING
        assertNull(userManager.telemetryUserId)

        userManager.trackingMode = InternalUserTrackingMode.ANONYMOUS_TRACKING
        val newUserId = userManager.telemetryUserId
        assertNotNull(newUserId)
        assertNotEquals(PENDING_USER_ID, newUserId)

        executor.runQueuedTasks()

        assertEquals("persisted-user-id", userManager.pendingUserId(wait = true))
        assertEquals(newUserId, userManager.userId)
        assertEquals(newUserId, state.anonymousUserId)
    }

    @Test
    fun `pending user id in span attributes is replaced once the user id is read`() {
        val (storage, _) = storageMock(anonymousUserId = "persisted-user-id")
        val executor = QueuedExecutor()
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage, executor)
        val attributes = Attributes.of(RumConstants.USER_ID_KEY, PENDING_USER_ID)

        assertSame(attributes, userManager.resolvePendingUserId(attributes, wait = false))

        executor.runQueuedTasks()

        val resolved = userManager.resolvePendingUserId(attributes, wait = true)
        assertEquals("persisted-user-id", resolved.get(RumConstants.USER_ID_KEY))
        assertEquals(attributes.size(), resolved.size())
    }

    @Test
    fun `pending user id is removed when the user id cannot be read`() {
        val storage = mock(IAgentStorage::class.java)
        `when`(storage.readAnonymousUserId()).thenThrow(IllegalStateException("storage failure"))
        val executor = QueuedExecutor()
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING, storage, executor)
        val attributes = Attributes.of(RumConstants.USER_ID_KEY, PENDING_USER_ID)

        executor.runQueuedTasks()

        assertNull(userManager.resolvePendingUserId(attributes, wait = true).get(RumConstants.USER_ID_KEY))
        assertNull(userManager.userId)
    }

    data class StorageState(var anonymousUserId: String? = null)

    private class QueuedExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()

        override fun execute(command: Runnable) {
            tasks.addLast(command)
        }

        fun runQueuedTasks() {
            while (tasks.isNotEmpty()) {
                tasks.removeFirst().run()
            }
        }
    }

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
