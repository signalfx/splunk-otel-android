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

package com.splunk.rum.bci.okhttp3;

import com.splunk.rum.library.okhttp3.tracing.OkHttpCallbackAdviceHelper;
import com.splunk.rum.library.okhttp3.tracing.TracingCallback;

import net.bytebuddy.asm.Advice;

import io.opentelemetry.context.Context;
import okhttp3.Call;
import okhttp3.Callback;

public class OkHttpCallbackAdvice {

    private OkHttpCallbackAdvice() {
        throw new IllegalStateException("Class not meant to be instantiated");
    }

    @Advice.OnMethodEnter
    public static void enter(
            @Advice.This Call call,
            @Advice.Argument(value = 0, readOnly = false) Callback callback) {
        if (OkHttpCallbackAdviceHelper.propagateContext(call)) {
            callback = new TracingCallback(callback, Context.current());
        }
    }
}