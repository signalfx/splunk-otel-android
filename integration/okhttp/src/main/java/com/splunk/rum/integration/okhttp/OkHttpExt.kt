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

package com.splunk.rum.integration.okhttp

import com.splunk.rum.integration.okhttp.interceptor.SplunkOkHttpInterceptor
import okhttp3.OkHttpClient

val OkHttpClient.Builder.splunkInterceptors: MutableList<SplunkOkHttpInterceptor>
    get() = InterceptionManager.instance.getOkHttpInterceptors(this)

fun OkHttpClient.Builder.addSplunkInterceptor(interceptor: SplunkOkHttpInterceptor): OkHttpClient.Builder {
    splunkInterceptors += interceptor
    return this
}

fun OkHttpClient.Builder.removeSplunkInterceptor(interceptor: SplunkOkHttpInterceptor): OkHttpClient.Builder {
    splunkInterceptors -= interceptor
    return this
}
