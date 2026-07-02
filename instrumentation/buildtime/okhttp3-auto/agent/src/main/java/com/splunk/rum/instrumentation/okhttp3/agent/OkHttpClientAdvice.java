/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.instrumentation.okhttp3.agent;

import com.splunk.rum.instrumentation.okhttp3.auto.internal.OkHttpSingletons;
import net.bytebuddy.asm.Advice;
import okhttp3.OkHttpClient;

public class OkHttpClientAdvice {

  @Advice.OnMethodEnter
  public static void enter(@Advice.Argument(0) OkHttpClient.Builder builder) {
    if (!builder.interceptors().contains(OkHttpSingletons.callbackContextInterceptor)) {
      builder.interceptors().add(0, OkHttpSingletons.callbackContextInterceptor);
      builder.interceptors().add(1, OkHttpSingletons.resendCountContextInterceptor);
      builder.interceptors().add(2, OkHttpSingletons.connectionErrorInterceptor);
    }
    if (!builder.networkInterceptors().contains(OkHttpSingletons.tracingInterceptor)) {
      builder.addNetworkInterceptor(OkHttpSingletons.tracingInterceptor);
    }
  }
}
