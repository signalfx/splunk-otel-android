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

package com.splunk.rum.agent.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumenterBuildUtilsTest {

    @Test
    fun `falls back when SPI configuration has a linkage error`() {
        var buildCalls = 0
        var fallbackBuildCalls = 0

        val result = InstrumenterBuildUtils.synchronizedInstrumenterBuild(
            configureSpiLookup = { throw NoSuchMethodError("setLoadFunction") },
            restoreSpiLookup = { error("restore must not be called") },
            build = {
                buildCalls++
                "normal"
            },
            fallbackBuild = {
                fallbackBuildCalls++
                "fallback"
            }
        )

        assertEquals("fallback", result)
        assertEquals(0, buildCalls)
        assertEquals(1, fallbackBuildCalls)
    }

    @Test
    fun `restores SPI configuration after a successful build`() {
        var restored = false

        val result = InstrumenterBuildUtils.synchronizedInstrumenterBuild(
            configureSpiLookup = {},
            restoreSpiLookup = { restored = true },
            build = { "normal" },
            fallbackBuild = { error("fallback must not be called") }
        )

        assertEquals("normal", result)
        assertTrue(restored)
    }
}
