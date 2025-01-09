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

package com.splunk.app.tests.e2e.networkrequests

import com.splunk.app.screens.HttpUrlConnectionScreen
import com.splunk.app.screens.MainScreen
import com.splunk.app.screens.OkHttpScreen
import com.splunk.app.screens.screen
import com.splunk.app.tests.e2e.BaseTestE2E
import org.junit.Test

class NetworkRequestsTest : BaseTestE2E() {

    @Test
    fun selectSeveralNetworkRequests() {
        screen<MainScreen> { okHttpSampleCallsButton.click() }

        screen<OkHttpScreen> {
            asynchronousGetButton.click()
            synchronousGetButton.click()
            postMarkdownButton.click()
            postFileButton.click()
            goBack()
        }

        screen<MainScreen> { httpUrlConnectionButton.click() }

        screen<HttpUrlConnectionScreen> {
            getCustomUrl("https://mock.httpstatus.io/200")
            getCustomUrl("https://mock.httpstatus.io/401")
            getCustomUrl("https://mock.httpstatus.io/404")
            getCustomUrl("https://mock.httpstatus.io/503")

            postButton.click()

            waitForSendingData()
        }
    }
}
