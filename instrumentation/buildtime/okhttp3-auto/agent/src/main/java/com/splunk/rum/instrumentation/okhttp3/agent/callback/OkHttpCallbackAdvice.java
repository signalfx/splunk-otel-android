/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.rum.instrumentation.okhttp3.agent.callback;

import com.splunk.rum.instrumentation.okhttp3.auto.internal.OkHttpCallbackAdviceHelper;
import com.splunk.rum.instrumentation.okhttp3.auto.internal.TracingCallback;
import io.opentelemetry.context.Context;
import net.bytebuddy.asm.Advice;
import okhttp3.Call;
import okhttp3.Callback;

public class OkHttpCallbackAdvice {

  @Advice.OnMethodEnter
  public static void enter(
      @Advice.This Call call,
      @Advice.Argument(value = 0, readOnly = false) Callback callback) {
    if (OkHttpCallbackAdviceHelper.propagateContext(call)) {
      callback = new TracingCallback(callback, Context.current());
    }
  }
}
