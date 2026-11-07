package com.splunk.rum.integration.agent.internal.session

import android.app.Activity
import android.app.Application
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.PREVIOUS_SESSION_ID_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SESSION_ID_KEY
import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.agent.common.storage.SessionId
import com.splunk.rum.common.utils.AppStateObserver
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Logger
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers.RETURNS_SELF
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SplunkSessionManagerTest {

    @After
    fun cleanUpObserver() {
        AppStateObserver.detach()
        AppStateObserver.listeners.clear()
    }

    @Test
    fun `creates new session when none stored`() {
        val storage = storageMock()
        val manager = SplunkSessionManager(storage.first, allowBackgroundSessionCreation = true)

        val createdId = manager.sessionId

        assertNotNull(createdId)
        assertEquals(createdId, storage.second.sessionId)
        assertNotNull("session validity should be persisted", storage.second.sessionValidUntil)
        assertEquals(1, storage.second.sessionIds.size)
        assertEquals(createdId, storage.second.sessionIds.first().id)
    }

    @Test
    @Ignore("Flaky on slow CI (5s validity window). Re-enable after fixing timing.")
    fun `reuses existing valid session`() {
        val now = System.currentTimeMillis()

        val (storage, state) = storageMock(
            sessionId = "existing-session",
            sessionValidUntil = now + 5_000,
            sessionValidUntilInBackground = now + 5_000,
            sessionIds = listOf(SessionId("existing-session", now - 1_000))
        )

        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = true)
        val sessionId = manager.sessionId

        assertEquals("existing-session", sessionId)
        assertNull("previous session should not be set when reusing", manager.previousSessionId)
        assertEquals(1, state.sessionIds.size)
    }

    @Test
    @Ignore("Flaky on slow CI (5s validity window). Re-enable after fixing timing.")
    fun `sessionStart returns current session start timestamp`() {
        val now = System.currentTimeMillis()
        val expectedSessionStart = now - 1_000
        val (storage, _) = storageMock(
            sessionId = "existing-session",
            sessionValidUntil = now + 5_000,
            sessionValidUntilInBackground = now + 5_000,
            sessionIds = listOf(SessionId("existing-session", expectedSessionStart))
        )

        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = true)

        assertEquals(expectedSessionStart, manager.sessionStart)
    }

    @Test
    @Ignore("Flaky on slow CI (5s validity window). Re-enable after fixing timing.")
    fun `sessionLastActivity falls back to sessionStart when no activity tracked`() {
        val now = System.currentTimeMillis()
        val expectedSessionStart = now - 1_000
        val (storage, _) = storageMock(
            sessionId = "existing-session",
            sessionValidUntil = now + 5_000,
            sessionValidUntilInBackground = now + 5_000,
            sessionIds = listOf(SessionId("existing-session", expectedSessionStart))
        )

        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = true)

        assertEquals(expectedSessionStart, manager.sessionLastActivity)
    }

    @Test
    @Ignore("Flaky on slow CI (5s validity window). Re-enable after fixing timing.")
    fun `sessionLastActivity returns tracked activity timestamp`() {
        val now = System.currentTimeMillis()
        val (storage, _) = storageMock(
            sessionId = "existing-session",
            sessionValidUntil = now + 5_000,
            sessionValidUntilInBackground = now + 5_000,
            sessionIds = listOf(SessionId("existing-session", now - 1_000))
        )
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = true)
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

        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = true)
        val newId = requireNotNull(manager.sessionId)

        assertNotEquals("expired-session", newId)
        assertEquals("expired-session", manager.previousSessionId)
        assertEquals(2, state.sessionIds.size)
        assertEquals(newId, state.sessionIds.last().id)
    }

    @Test
    fun `pending start is ready before session listeners produce telemetry`() {
        val now = System.currentTimeMillis()
        val (storage, state) = storageMock(
            sessionId = "expired-session",
            sessionValidUntil = now - 1_000,
            sessionIds = listOf(SessionId("expired-session", now - 2_000))
        )
        val recordBuilder = mock(LogRecordBuilder::class.java, RETURNS_SELF)
        val logger = mock(Logger::class.java)
        `when`(logger.logRecordBuilder()).thenReturn(recordBuilder)
        val emitter = SessionStartEmitter().apply { attach(logger) }
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = true) { id, previousId, timestamp ->
            emitter.onSessionCreated(id, previousId, timestamp)
        }
        manager.sessionListeners += object : SplunkSessionManager.SessionListener {
            override fun onSessionChanged(sessionId: String, timestamp: Long) {
                emitter.emitIfPending(sessionId)
            }
        }

        val newId = requireNotNull(manager.sessionId)

        verify(recordBuilder).setAttribute(SESSION_ID_KEY, newId)
        verify(recordBuilder).setAttribute(PREVIOUS_SESSION_ID_KEY, "expired-session")
        verify(recordBuilder).setTimestamp(state.sessionIds.last().validFrom, TimeUnit.MILLISECONDS)
        verify(recordBuilder).emit()
    }

    @Test
    fun `reset clears stored session`() {
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = true)
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
        val manager = SplunkSessionManager(storage.first, allowBackgroundSessionCreation = true)

        assertEquals("first", manager.sessionId(150))
        assertEquals("second", manager.sessionId(300))
        assertEquals("latest", manager.sessionId(1_000))
    }

    @Test
    fun `sessionId has no value when no matching timestamp`() {
        val storage = storageMock(
            sessionIds = listOf(SessionId("only", 500))
        )
        val manager = SplunkSessionManager(storage.first, allowBackgroundSessionCreation = true)

        assertNull(manager.sessionId(100))
    }

    @Test
    fun `background timeout triggers new session creation`() {
        val now = System.currentTimeMillis()
        val (storage, state) = storageMock(
            sessionId = "active-session",
            sessionValidUntil = now + 10_000,
            sessionIds = listOf(SessionId("active-session", now))
        )
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = true)
        manager.sessionBackgroundTimeout = 10L
        manager.watchSessionInBackgroundValidity()

        Thread.sleep(30) // allow scheduled task to fire

        val newId = storage.readSessionId()
        assertNotNull(newId)
        assertNotEquals("active-session", newId)
        assertEquals(2, state.sessionIds.size)
        assertTrue("background validity should be cleared", state.sessionValidUntilInBackground == null)
    }

    @Test
    fun `foreground-aware mode stays sessionless during a headless start and ID reads`() {
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = false)

        manager.reset()
        manager.install(RuntimeEnvironment.getApplication())

        assertNull(manager.sessionId)
        assertNull(manager.sessionSnapshot)
        assertNull(manager.sessionStart)
        manager.trackSessionActivity()
        assertNull(state.sessionId)
        assertNull(state.sessionLastActivity)
        assertTrue(state.sessionIds.isEmpty())
    }

    @Test
    fun `first started Activity creates one session and duplicate callbacks do not create another`() {
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = false)
        manager.reset()
        manager.install(RuntimeEnvironment.getApplication())

        val activity = Robolectric.buildActivity(Activity::class.java).create()
        assertNull(manager.sessionId)
        activity.start().resume()

        val firstId = manager.sessionId
        assertNotNull(firstId)
        AppStateObserver.listeners.forEach { it.onAppForegrounded() }
        assertEquals(firstId, manager.sessionId)
        assertEquals(1, state.sessionIds.size)

        activity.pause().stop().destroy()
    }

    @Test
    fun `late install uses the already observed started Activity`() {
        val application: Application = RuntimeEnvironment.getApplication()
        AppStateObserver.attach(application)
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = false)

        manager.reset()
        manager.install(application)

        assertNotNull(manager.sessionId)
        assertEquals(1, state.sessionIds.size)
        activity.pause().stop().destroy()
    }

    @Test
    fun `background deadline is fixed and returning before it keeps the session`() {
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = false)
        manager.reset()
        manager.install(RuntimeEnvironment.getApplication())
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val firstId = manager.sessionId

        activity.pause().stop()
        val deadline = state.sessionValidUntilInBackground
        assertNotNull(deadline)
        manager.trackSessionActivity()
        assertEquals(deadline, state.sessionValidUntilInBackground)
        activity.start().resume()
        manager.expireSessionInBackground() // delayed task from the previous background period

        assertEquals(firstId, manager.sessionId)
        assertNull(state.sessionValidUntilInBackground)
        assertEquals(1, state.sessionIds.size)
        activity.pause().stop().destroy()
    }

    @Test
    fun `destroying the last Activity does not extend the background deadline`() {
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = false)
        manager.reset()
        manager.install(RuntimeEnvironment.getApplication())
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().resume()

        activity.pause().stop()
        val deadline = state.sessionValidUntilInBackground
        activity.destroy()

        assertNotNull(deadline)
        assertEquals(deadline, state.sessionValidUntilInBackground)
    }

    @Test
    fun `expired background session is cleared without a replacement`() {
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = false)
        manager.reset()
        manager.install(RuntimeEnvironment.getApplication())
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val firstId = manager.sessionId

        activity.pause().stop()
        state.sessionValidUntilInBackground = System.currentTimeMillis() - 1
        manager.expireSessionInBackground()

        assertNull(manager.sessionId)
        assertNull(state.sessionId)
        assertEquals(1, state.sessionIds.size)

        activity.start().resume()
        assertNotEquals(firstId, manager.sessionId)
        assertNull(manager.previousSessionId)
        assertEquals(2, state.sessionIds.size)
        activity.pause().stop().destroy()
    }

    @Test
    fun `four hour expiry rotates only while an Activity is started`() {
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = false)
        manager.reset()
        manager.install(RuntimeEnvironment.getApplication())
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val firstId = requireNotNull(manager.sessionId)
        state.sessionValidUntil = System.currentTimeMillis() - 1

        assertNull("an ID read cannot rotate the session", manager.sessionId)
        assertEquals(firstId, state.sessionId)
        manager.rotateSessionIfNeeded(firstId)

        assertNotEquals(firstId, manager.sessionId)
        assertEquals(firstId, manager.previousSessionId)
        assertEquals(2, state.sessionIds.size)
        activity.pause().stop().destroy()
    }

    @Test
    fun `four hour expiry in the background does not create a replacement`() {
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = false)
        manager.reset()
        manager.install(RuntimeEnvironment.getApplication())
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val firstId = requireNotNull(manager.sessionId)

        activity.pause().stop()
        state.sessionValidUntil = System.currentTimeMillis() - 1
        manager.rotateSessionIfNeeded(firstId)

        assertNull(state.sessionId)
        assertNull(manager.sessionId)
        assertEquals(1, state.sessionIds.size)
        activity.destroy()
    }

    @Test
    fun `process restart clears the old ID and waits for the next foreground`() {
        val (storage, state) = storageMock()
        val original = SplunkSessionManager(storage, allowBackgroundSessionCreation = false)
        original.reset()
        original.install(RuntimeEnvironment.getApplication())
        val firstActivity = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val firstId = original.sessionId
        firstActivity.pause().stop().destroy()
        AppStateObserver.listeners.clear() // process death drops the previous manager and its listeners
        AppStateObserver.detach()

        val restarted = SplunkSessionManager(storage, allowBackgroundSessionCreation = false)
        restarted.reset()
        restarted.install(RuntimeEnvironment.getApplication())
        assertNull(restarted.sessionId)
        assertNull(state.sessionId)

        val secondActivity = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        assertNotEquals(firstId, restarted.sessionId)
        assertNull(restarted.previousSessionId)
        secondActivity.pause().stop().destroy()
    }

    @Test
    fun `compatibility mode still creates a session at install without an Activity`() {
        val (storage, state) = storageMock()
        val manager = SplunkSessionManager(storage, allowBackgroundSessionCreation = true)

        manager.install(RuntimeEnvironment.getApplication())

        assertNotNull(manager.sessionId)
        assertFalse(state.sessionIds.isEmpty())
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
