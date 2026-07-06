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
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Timeout;

class TracingCallFactory implements Call.Factory {

  private final OkHttpClient okHttpClient;

  TracingCallFactory(OkHttpClient okHttpClient) {
    this.okHttpClient = okHttpClient;
  }

  @Nullable
  static Context getCallingContextForRequest(Request request) {
    return request.tag(Context.class);
  }

  @Override
  public Call newCall(Request request) {
    Context callingContext = Context.current();
    Request requestWithContext = attachContextToRequest(request, callingContext);
    return new TracingCall(okHttpClient, okHttpClient.newCall(requestWithContext), callingContext);
  }

  private static Request attachContextToRequest(Request request, Context context) {
    return request.newBuilder().tag(Context.class, context).build();
  }

  static class TracingCall implements Call {
    private final OkHttpClient okHttpClient;
    private final Call delegate;
    private final Context callingContext;

    TracingCall(OkHttpClient okHttpClient, Call delegate, Context callingContext) {
      this.okHttpClient = okHttpClient;
      this.delegate = delegate;
      this.callingContext = callingContext;
    }

    @Override
    public void cancel() {
      delegate.cancel();
    }

    @Override
    public Call clone() {
      // Cloning may happen under a different context than the original call creation.
      Context currentContext = Context.current();
      Request updatedRequest = attachContextToRequest(delegate.request(), currentContext);
      return new TracingCall(okHttpClient, okHttpClient.newCall(updatedRequest), currentContext);
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

    @Override
    public Timeout timeout() {
      return delegate.timeout();
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
