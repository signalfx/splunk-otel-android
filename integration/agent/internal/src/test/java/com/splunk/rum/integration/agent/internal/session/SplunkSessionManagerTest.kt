package com.splunk.rum.integration.agent.internal.session

import com.splunk.rum.common.storage.IAgentStorage
import com.splunk.rum.common.storage.SessionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SplunkSessionManagerTest {

    @Test
    fun `creates new session when none stored`() {
        val storage = storageMock()
        val manager = SplunkSessionManager(storage.first)

        val createdId = manager.sessionId

        assertNotNull(createdId)
        assertEquals(createdId, storage.second.sessionId)
        assertNotNull("session validity should be persisted", storage.second.sessionValidUntil)
        assertEquals(1, storage.second.sessionIds.size)
        assertEquals(createdId, storage.second.sessionIds.first().id)
    }

    @Test
    fun `reuses existing valid session`() {
        val now = System.currentTimeMillis()

        val (storage, state) = storageMock(
            sessionId = "existing-session",
            sessionValidUntil = now + 5_000,
            sessionValidUntilInBackground = now + 5_000,
            sessionIds = listOf(SessionId("existing-session", now - 1_000))
        )

        val manager = SplunkSessionManager(storage)
        val sessionId = manager.sessionId

        assertEquals("existing-session", sessionId)
        assertNull("previous session should not be set when reusing", manager.previousSessionId)
        assertEquals(1, state.sessionIds.size)
    }

    @Test
    fun `sessionStart returns current session start timestamp`() {
        val now = System.currentTimeMillis()
        val expectedSessionStart = now - 1_000
        val (storage, _) = storageMock(
            sessionId = "existing-session",
            sessionValidUntil = now + 5_000,
            sessionValidUntilInBackground = now + 5_000,
            sessionIds = listOf(SessionId("existing-session", expectedSessionStart))
        )

        val manager = SplunkSessionManager(storage)

        assertEquals(expectedSessionStart, manager.sessionStart)
    }

    @Test
    fun `sessionLastActivity falls back to sessionStart when no activity tracked`() {
        val now = System.currentTimeMillis()
        val expectedSessionStart = now - 1_000
        val (storage, _) = storageMock(
            sessionId = "existing-session",
            sessionValidUntil = now + 5_000,
            sessionValidUntilInBackground = now + 5_000,
            sessionIds = listOf(SessionId("existing-session", expectedSessionStart))
        )

        val manager = SplunkSessionManager(storage)

        assertEquals(expectedSessionStart, manager.sessionLastActivity)
    }

    @Test
    fun `sessionLastActivity returns tracked activity timestamp`() {
        val now = System.currentTimeMillis()
        val (storage, _) = storageMock(
            sessionId = "existing-session",
            sessionValidUntil = now + 5_000,
            sessionValidUntilInBackground = now + 5_000,
            sessionIds = listOf(SessionId("existing-session", now - 1_000))
        )
        val manager = SplunkSessionManager(storage)
        val before = System.currentTimeMillis()

        manager.trackSessionActivity()

        val after = System.currentTimeMillis()
        assertTrue(manager.sessionLastActivity in before..after)
    }

    @Test
    fun `creates new session when stored one is expired`() {
        val now = System.currentTimeMillis()
        val (storage, state) = storageMock(
            sessionId = "expired-session",
            sessionValidUntil = now - 1_000,
            sessionIds = listOf(SessionId("expired-session", now - 2_000))
        )

        val manager = SplunkSessionManager(storage)
        val newId = manager.sessionId

        assertNotEquals("expired-session", newId)
        assertEquals("expired-session", manager.previousSessionId)
        assertEquals(2, state.sessionIds.size)
        assertEquals(newId, state.sessionIds.last().id)
    }

    @Test
    fun `reset clears stored session`() {
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage)
        manager.sessionId // trigger creation

        manager.reset()

        assertNull(state.sessionId)
        assertNull(state.sessionValidUntil)
        assertNull(state.sessionValidUntilInBackground)
    }

    @Test
    fun `sessionId returns correct id for timestamp`() {
        val storage = storageMock(
            sessionIds = listOf(
                SessionId("first", 100),
                SessionId("second", 200),
                SessionId("latest", 500)
            )
        )
        val manager = SplunkSessionManager(storage.first)

        assertEquals("first", manager.sessionId(150))
        assertEquals("second", manager.sessionId(300))
        assertEquals("latest", manager.sessionId(1_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sessionId throws when no matching timestamp`() {
        val storage = storageMock(
            sessionIds = listOf(SessionId("only", 500))
        )
        val manager = SplunkSessionManager(storage.first)

        manager.sessionId(100)
    }

    @Test
    fun `background timeout triggers new session creation`() {
        val now = System.currentTimeMillis()
        val (storage, state) = storageMock(
            sessionId = "active-session",
            sessionValidUntil = now + 10_000,
            sessionIds = listOf(SessionId("active-session", now))
        )
        val manager = SplunkSessionManager(storage)
        manager.sessionBackgroundTimeout = 10L
        manager.watchSessionInBackgroundValidity()

        Thread.sleep(30) // allow scheduled task to fire

        val newId = storage.readSessionId()
        assertNotNull(newId)
        assertNotEquals("active-session", newId)
        assertEquals(2, state.sessionIds.size)
        assertTrue("background validity should be cleared", state.sessionValidUntilInBackground == null)
    }
    data class StorageState(
        var sessionId: String? = null,
        var sessionValidUntil: Long? = null,
        var sessionValidUntilInBackground: Long? = null,
        var sessionLastActivity: Long? = null,
        var sessionIds: List<SessionId> = emptyList()
    )

    private fun storageMock(
        sessionId: String? = null,
        sessionValidUntil: Long? = null,
        sessionValidUntilInBackground: Long? = null,
        sessionLastActivity: Long? = null,
        sessionIds: List<SessionId> = emptyList()
    ): Pair<IAgentStorage, StorageState> {
        val state = StorageState(
            sessionId = sessionId,
            sessionValidUntil = sessionValidUntil,
            sessionValidUntilInBackground = sessionValidUntilInBackground,
            sessionLastActivity = sessionLastActivity,
            sessionIds = sessionIds
        )

        val storage = mock(IAgentStorage::class.java)

        `when`(storage.readSessionId()).thenAnswer { state.sessionId }
        doAnswer { invocation ->
            state.sessionId = invocation.getArgument(0)
            null
        }.`when`(storage).writeSessionId(anyString())

        doAnswer {
            state.sessionId = null
            null
        }.`when`(storage).deleteSessionId()

        `when`(storage.readSessionValidUntil()).thenAnswer { state.sessionValidUntil }
        doAnswer { invocation ->
            state.sessionValidUntil = invocation.getArgument(0)
            null
        }.`when`(storage).writeSessionValidUntil(anyLong())
        doAnswer {
            state.sessionValidUntil = null
            null
        }.`when`(storage).deleteSessionValidUntil()

        `when`(storage.readSessionValidUntilInBackground()).thenAnswer { state.sessionValidUntilInBackground }
        doAnswer { invocation ->
            state.sessionValidUntilInBackground = invocation.getArgument(0)
            null
        }.`when`(storage).writeSessionValidUntilInBackground(anyLong())
        doAnswer {
            state.sessionValidUntilInBackground = null
            null
        }.`when`(storage).deleteSessionValidUntilInBackground()

        `when`(storage.readSessionLastActivity()).thenAnswer { state.sessionLastActivity }
        doAnswer { invocation ->
            state.sessionLastActivity = invocation.getArgument(0)
            null
        }.`when`(storage).writeSessionLastActivity(anyLong())
        doAnswer {
            state.sessionLastActivity = null
            null
        }.`when`(storage).deleteSessionLastActivity()

        `when`(storage.readSessionIds()).thenAnswer { state.sessionIds }
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            state.sessionIds = (invocation.getArgument(0) as List<SessionId>).toList()
            null
        }.`when`(storage).writeSessionIds(org.mockito.ArgumentMatchers.anyList())

        return storage to state
    }
}
