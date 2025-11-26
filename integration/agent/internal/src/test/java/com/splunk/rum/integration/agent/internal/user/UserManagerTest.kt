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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UserManagerTest {

    @Test
    fun `initial user id is null when tracking disabled`() {
        val userManager = UserManager(InternalUserTrackingMode.NO_TRACKING)

        assertEquals(InternalUserTrackingMode.NO_TRACKING, userManager.trackingMode)
        assertNull(userManager.userId)
    }

    @Test
    fun `initial user id is generated when anonymous tracking enabled`() {
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING)

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertNotNull(userManager.userId)
    }

    @Test
    fun `switching to no tracking clears user id`() {
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING)
        assertNotNull(userManager.userId)

        userManager.trackingMode = InternalUserTrackingMode.NO_TRACKING

        assertEquals(InternalUserTrackingMode.NO_TRACKING, userManager.trackingMode)
        assertNull(userManager.userId)
    }

    @Test
    fun `switching between anonymous tracking preserves user id`() {
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING)
        val initialUserId = userManager.userId

        userManager.trackingMode = InternalUserTrackingMode.ANONYMOUS_TRACKING

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertEquals(initialUserId, userManager.userId)
    }

    @Test
    fun `disabling and re enabling anonymous tracking generates new id`() {
        val userManager = UserManager(InternalUserTrackingMode.ANONYMOUS_TRACKING)
        val initialUserId = userManager.userId

        userManager.trackingMode = InternalUserTrackingMode.NO_TRACKING
        assertNull(userManager.userId)

        userManager.trackingMode = InternalUserTrackingMode.ANONYMOUS_TRACKING

        assertEquals(InternalUserTrackingMode.ANONYMOUS_TRACKING, userManager.trackingMode)
        assertNotNull(userManager.userId)
        assertNotEquals(initialUserId, userManager.userId)
    }
}
