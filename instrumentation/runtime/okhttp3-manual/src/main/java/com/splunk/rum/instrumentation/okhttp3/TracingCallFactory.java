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

package com.splunk.rum.instrumentation.okhttp3;

import androidx.annotation.Nullable;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Timeout;

class TracingCallFactory implements Call.Factory {

  private static final VirtualField<Request, Context> contextsByRequest =
      VirtualField.find(Request.class, Context.class);

  // We use old-school reflection here, rather than MethodHandles because Android doesn't support
  // MethodHandles until API 26.
  @Nullable private static Method timeoutMethod;
  @Nullable private static Method cloneMethod;

  static {
    try {
      timeoutMethod = Call.class.getMethod("timeout");
    } catch (NoSuchMethodException e) {
      timeoutMethod = null;
    }
    try {
      cloneMethod = Call.class.getDeclaredMethod("clone");
    } catch (NoSuchMethodException e) {
      cloneMethod = null;
    }
  }

  private final OkHttpClient okHttpClient;

  TracingCallFactory(OkHttpClient okHttpClient) {
    this.okHttpClient = okHttpClient;
  }

  @Nullable
  static Context getCallingContextForRequest(Request request) {
    return contextsByRequest.get(request);
  }

  @Override
  public Call newCall(Request request) {
    Context callingContext = Context.current();
    Request requestCopy = request.newBuilder().build();
    contextsByRequest.set(requestCopy, callingContext);
    return new TracingCall(okHttpClient.newCall(requestCopy), callingContext);
  }

  static class TracingCall implements Call {
    private final Call delegate;
    private final Context callingContext;

    TracingCall(Call delegate, Context callingContext) {
      this.delegate = delegate;
      this.callingContext = callingContext;
    }

    @Override
    public void cancel() {
      delegate.cancel();
    }

    @Override
    public Call clone() {
      if (cloneMethod == null) {
        return new TracingCall(delegate.clone(), Context.current());
      }
      try {
        // we pull the current context here, because the cloning might be happening in a different
        // context than the original call creation.
        return new TracingCall((Call) cloneMethod.invoke(delegate), Context.current());
      } catch (IllegalAccessException | InvocationTargetException e) {
        return new TracingCall(delegate.clone(), Context.current());
      }
    }

    @Override
    public void enqueue(Callback callback) {
      delegate.enqueue(new TracingCallback(callback, callingContext));
    }

    @Override
    public Response execute() throws IOException {
      try (Scope scope = callingContext.makeCurrent()) {
        return delegate.execute();
      }
    }

    @Override
    public boolean isCanceled() {
      return delegate.isCanceled();
    }

    @Override
    public boolean isExecuted() {
      return delegate.isExecuted();
    }

    @Override
    public Request request() {
      return delegate.request();
    }

    // @Override method was introduced in 3.12
    public Timeout timeout() {
      if (timeoutMethod == null) {
        return Timeout.NONE;
      }
      try {
        return (Timeout) timeoutMethod.invoke(delegate);
      } catch (IllegalAccessException | InvocationTargetException e) {
        // do nothing...we're before 3.12, or something else has gone wrong that we can't do
        // anything about.
        return Timeout.NONE;
      }
    }

    private static class TracingCallback implements Callback {
      private final Callback delegate;
      private final Context callingContext;

      TracingCallback(Callback delegate, Context callingContext) {
        this.delegate = delegate;
        this.callingContext = callingContext;
      }

      @Override
      public void onFailure(Call call, IOException e) {
        try (Scope scope = callingContext.makeCurrent()) {
          delegate.onFailure(call, e);
        }
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        try (Scope scope = callingContext.makeCurrent()) {
          delegate.onResponse(call, response);
        }
      }
    }
  }
}
