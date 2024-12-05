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

package com.smartlook.app.tests.component.networkrequests

import com.smartlook.app.lib.ZipkinCommunicator
import com.smartlook.app.screens.HttpUrlConnectionScreen
import com.smartlook.app.screens.MainScreen
import com.smartlook.app.screens.screen
import com.smartlook.app.tests.component.BaseTest
import org.junit.Before
import org.junit.Test

class HttpUrlConnectionTest : BaseTest() {
    private var startingMillis: Long = System.currentTimeMillis()
    @Before
    fun getIntoScreen() {
        screen<MainScreen> {
            httpUrlConnectionButton.click()
            startingMillis = System.currentTimeMillis()
        }
    }

    @Test
    fun testGetRequest() {
        screen<HttpUrlConnectionScreen> {
            successfulGetButton.click()
            waitForSendingData()
        }

        NetworkRequestsUtil.verifyBySpanNameWithZipkin(
            method = "GET",
            duration = ZipkinCommunicator.getTestDuration(startingMillis)
        )
    }

    @Test
    fun testPostRequest() {
        screen<HttpUrlConnectionScreen> {
            postButton.click()
            waitForSendingData()
        }

        NetworkRequestsUtil.verifyBySpanNameWithZipkin(
            method = "POST",
            duration = ZipkinCommunicator.getTestDuration(startingMillis)
        )
    }
}
