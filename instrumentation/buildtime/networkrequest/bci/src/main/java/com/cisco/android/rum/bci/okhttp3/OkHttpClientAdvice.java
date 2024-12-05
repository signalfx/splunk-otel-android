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

package com.cisco.android.rum.bci.okhttp3;

import com.cisco.android.rum.library.okhttp3.OkHttp3Singletons;

import net.bytebuddy.asm.Advice;

import okhttp3.OkHttpClient;

public class OkHttpClientAdvice {

    private OkHttpClientAdvice() {
        throw new IllegalStateException("Class not meant to be instantiated");
    }

    @Advice.OnMethodEnter
    public static void enter(@Advice.Argument(0) OkHttpClient.Builder builder) {
        if (!builder.interceptors().contains(OkHttp3Singletons.CALLBACK_CONTEXT_INTERCEPTOR)) {
            builder.interceptors().add(0, OkHttp3Singletons.CALLBACK_CONTEXT_INTERCEPTOR);
            builder.interceptors().add(1, OkHttp3Singletons.RESEND_COUNT_CONTEXT_INTERCEPTOR);
            builder.interceptors().add(2, OkHttp3Singletons.CONNECTION_ERROR_INTERCEPTOR);
        }

        if (!builder.networkInterceptors().contains(OkHttp3Singletons.TRACING_INTERCEPTOR)) {
            builder.addNetworkInterceptor(OkHttp3Singletons.TRACING_INTERCEPTOR);
        }
    }
}