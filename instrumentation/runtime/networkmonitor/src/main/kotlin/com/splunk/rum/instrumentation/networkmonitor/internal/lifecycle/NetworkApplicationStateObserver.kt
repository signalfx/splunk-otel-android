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

package com.splunk.rum.instrumentation.networkmonitor.internal.lifecycle

import android.app.Application
import com.splunk.android.common.utils.AppStateObserver
import java.io.Closeable

internal class NetworkApplicationStateObserver(application: Application, listener: AppStateObserver.Listener) :
    Closeable {
    private val observer = AppStateObserver().apply {
        this.listener = listener
        attach(application)
    }

    override fun close() {
        observer.detach()
    }
}
