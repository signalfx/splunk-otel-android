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

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import okhttp3.Call;
import okhttp3.Request;

public final class OkHttpCallbackAdviceHelper {

    private OkHttpCallbackAdviceHelper() {
        throw new IllegalStateException("Class not meant to be instantiated");
    }

    public static boolean propagateContext(Call call) {
        Context context = Context.current();
        if (shouldPropagateContext(context)) {
            VirtualField<Request, Context> virtualField =
                    VirtualField.find(Request.class, Context.class);
            virtualField.set(call.request(), context);
            return true;
        }

        return false;
    }

    public static Context tryRecoverPropagatedContextFromCallback(Request request) {
        VirtualField<Request, Context> virtualField =
                VirtualField.find(Request.class, Context.class);
        return virtualField.get(request);
    }

    private static boolean shouldPropagateContext(Context context) {
        return context != Context.root();
    }
}
