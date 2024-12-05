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

package com.cisco.android.rum.library.okhttp3.tracing;

import java.io.IOException;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class TracingInterceptor implements Interceptor {

    private final Instrumenter<Request, Response> instrumenter;
    private final ContextPropagators propagators;

    private static final String TAG = "TracingInterceptor";

    public TracingInterceptor(
            Instrumenter<Request, Response> instrumenter, ContextPropagators propagators) {
        this.instrumenter = instrumenter;
        this.propagators = propagators;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        if (!OkHttpInterceptorUtils.isTracingEnabledAtInitiation(TAG, request)) {
            return chain.proceed(request);
        }

        Context parentContext = Context.current();

        if (!instrumenter.shouldStart(parentContext, request)) {
            return chain.proceed(chain.request());
        }

        Context context = instrumenter.start(parentContext, request);
        request = injectContextToRequest(request, context);

        Response response = null;
        Throwable error = null;
        try (Scope ignored = context.makeCurrent()) {
            response = chain.proceed(request);
            return response;
        } catch (Exception e) {
            error = e;
            throw e;
        } finally {
            instrumenter.end(context, request, response, error);
        }
    }

    private Request injectContextToRequest(Request request, Context context) {
        Request.Builder requestBuilder = request.newBuilder();
        propagators
                .getTextMapPropagator()
                .inject(context, requestBuilder, RequestPropertySetter.INSTANCE);
        return requestBuilder.build();
    }
}
