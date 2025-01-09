/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.rum.anr

import android.os.Handler
import com.splunk.rum.anr.ANRWatchDog
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.concurrent.thread

class ANRWatchDogTest {

    private class TestSetup {
        val uiHandler: Handler = mockk(relaxed = true)
        val anrListener: ANRWatchDog.ANRListener = mockk(relaxed = true)
        val anrWatchDog = ANRWatchDog(uiHandler, anrListener, 5L, true)
    }

    private fun test(block: TestSetup.() -> Unit) {
        val testSetup = TestSetup()
        block.invoke(testSetup)
    }

    @Test
    fun `when anr above threshold`() = test {
        givenUIThreadDoesNotDisappearAndCountdownDoesNotHappen()

        whenANRWatchDogRunsXTimes(5)

        thenAnrDetected()
    }

    @Test
    fun `when anr below threshold`() = test {
        givenUIThreadDoesNotDisappearAndCountdownDoesNotHappen()

        whenANRWatchDogRunsXTimes(4)

        thenNoAnrDetected()
    }

    @Test
    fun `when mainThreadDisappears`() = test {
        givenUIThreadDisappears()

        whenANRWatchDogRunsXTimes(5)

        thenNoAnrDetected()
    }

    @Test
    fun `when no ANR occurs`() = test {
        givenOrdinaryUIThreadExecutionWhereCountdownLatchIsBeingDecremented()

        whenANRWatchDogRunsXTimes(5)

        thenNoAnrDetected()
    }

    @Test
    fun `when temporary pause occurs`() = test {
        repeat(5) { index ->
            if (index == 0) {
                givenUIThreadDoesNotDisappearAndCountdownDoesNotHappen()
            } else {
                givenOrdinaryUIThreadExecutionWhereCountdownLatchIsBeingDecremented()
            }
        }
        thenNoAnrDetected()
    }

    private fun TestSetup.givenOrdinaryUIThreadExecutionWhereCountdownLatchIsBeingDecremented() {
        every { uiHandler.post(any()) } answers {
            val runnable = arg<Runnable>(0)
            thread {
                runnable.run()
            }
            true
        }
    }

    private fun TestSetup.givenUIThreadDisappears() {
        every { uiHandler.post(any()) } returns false
    }

    private fun TestSetup.givenUIThreadDoesNotDisappearAndCountdownDoesNotHappen() {
        every { uiHandler.post(any()) } returns true
    }

    private fun TestSetup.whenANRWatchDogRunsXTimes(timesToRun: Int) {
        repeat(timesToRun) {
            anrWatchDog.run()
        }
    }

    private fun TestSetup.thenNoAnrDetected() {
        verify(exactly = 0) { anrListener.onAppNotResponding() }
    }

    private fun TestSetup.thenAnrDetected() {
        verify(exactly = 1) { anrListener.onAppNotResponding() }
    }
}
