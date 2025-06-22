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

package com.splunk.app.screens

import android.view.View
import com.splunk.app.R
import org.hamcrest.Matcher

class HttpUrlConnectionScreen : Screen() {

    /**
     * EditTexts
     */
    private val customUrlInputEditText: Matcher<View> = view(R.id.customUrl)

    /**
     * Buttons
     */
    private val customUrlGetButton: Matcher<View> = view(R.id.customUrlGet)
    val successfulGetButton: Matcher<View> = view(R.id.successfulGet)
    val unsuccessfulGetButton: Matcher<View> = view(R.id.unSuccessfulGet)
    val getWithoutInputStreamButton: Matcher<View> = view(R.id.getWithoutInputStream)
    val fourConcurrentGetRequestsButton: Matcher<View> = view(R.id.fourConcurrentGetRequests)
    val postButton: Matcher<View> = view(R.id.post)
    val sustainedConnectionButton: Matcher<View> = view(R.id.sustainedConnection)
    val stalledRequestButton: Matcher<View> = view(R.id.stalledRequest)

    fun getCustomUrl(url: String) {
        screen<HttpUrlConnectionScreen> {
            customUrlInputEditText.replaceText(url)
            customUrlGetButton.click()
        }
    }
}
