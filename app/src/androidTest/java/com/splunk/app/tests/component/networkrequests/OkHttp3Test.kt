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

package com.splunk.app.tests.component.networkrequests

import com.splunk.app.lib.ZipkinCommunicator
import com.splunk.app.screens.MainScreen
import com.splunk.app.screens.OkHttpScreen
import com.splunk.app.screens.screen
import com.splunk.app.tests.component.BaseTest
import org.junit.Before
import org.junit.Test

class OkHttp3Test : BaseTest() {
    private var startingMillis: Long = System.currentTimeMillis()

    @Before
    fun getIntoOkHttpScreen() {
        screen<MainScreen> {
            okHttpSampleCallsButton.click()
        }
    }

    @Test
    fun testGetRequest() {
        screen<OkHttpScreen> {
            asynchronousGetButton.click()
            waitForSendingData()
        }

        NetworkRequestsUtil.verifyBySpanNameWithZipkin(
            method = "GET",
            duration = ZipkinCommunicator.getTestDuration(startingMillis)
        )
    }

    @Test
    fun testPostRequest() {
        screen<OkHttpScreen> {
            postMarkdownButton.click()
            waitForSendingData()
        }

        NetworkRequestsUtil.verifyBySpanNameWithZipkin(
            method = "POST",
            duration = ZipkinCommunicator.getTestDuration(startingMillis)
        )
    }
}
