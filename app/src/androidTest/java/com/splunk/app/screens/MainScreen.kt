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

class MainScreen : Screen() {

    /**
     * Buttons
     */
    val okHttpSampleCallsButton: Matcher<View> = view(R.id.okhttp_sample_calls)
    val httpUrlConnectionButton: Matcher<View> = view(R.id.httpurlconnection)
}
