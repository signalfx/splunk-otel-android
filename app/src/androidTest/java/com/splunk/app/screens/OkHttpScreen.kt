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

class OkHttpScreen : Screen() {

    /**
     * Buttons
     */
    val synchronousGetButton: Matcher<View> = view(R.id.synchronousGet)
    val asynchronousGetButton: Matcher<View> = view(R.id.asynchronousGet)
    val multipleHeadersButton: Matcher<View> = view(R.id.multipleHeaders)
    val postMarkdownButton: Matcher<View> = view(R.id.postMarkdown)
    val postStreamingButton: Matcher<View> = view(R.id.postStreaming)
    val postFileButton: Matcher<View> = view(R.id.postFile)
    val postFromParametersButton: Matcher<View> = view(R.id.postFormParameters)
    val postMultipartRequestButton: Matcher<View> = view(R.id.postMutlipartRequest)
    val responseCachingButton: Matcher<View> = view(R.id.responseCaching)
    val canceledCallButton: Matcher<View> = view(R.id.canceledCall)
}
