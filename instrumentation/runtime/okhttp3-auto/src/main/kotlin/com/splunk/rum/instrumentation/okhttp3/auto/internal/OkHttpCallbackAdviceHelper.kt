/*
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.okhttp3.auto.internal

import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.util.VirtualField
import okhttp3.Call
import okhttp3.Request

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
internal object OkHttpCallbackAdviceHelper {

    @JvmStatic
    fun propagateContext(call: Call): Boolean {
        val context = Context.current()
        if (shouldPropagateContext(context)) {
            val virtualField = VirtualField.find(
                Request::class.java,
                Context::class.java
            )
            virtualField.set(call.request(), context)
            return true
        }
        return false
    }

    @JvmStatic
    fun tryRecoverPropagatedContextFromCallback(request: Request): Context? {
        val virtualField = VirtualField.find(
            Request::class.java,
            Context::class.java
        )
        return virtualField.get(request)
    }

    private fun shouldPropagateContext(context: Context): Boolean = context != Context.root()
}
