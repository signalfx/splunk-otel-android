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

package com.splunk.rum.integration.lifecycle.screen.activity

import android.app.Activity
import com.splunk.rum.integration.lifecycle.screen.VisibleScreenTracker

internal class ActivityCallback29(override val tracker: VisibleScreenTracker) : ActivityCallback {

    override fun onActivityPostResumed(activity: Activity) {
        tracker.onActivityResumed(activity)
    }

    override fun onActivityPrePaused(activity: Activity) {
        tracker.onActivityPaused(activity)
    }
}
