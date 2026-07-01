/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
