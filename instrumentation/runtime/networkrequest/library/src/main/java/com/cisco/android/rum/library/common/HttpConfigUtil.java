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

package com.cisco.android.rum.library.common;

import android.util.Log;

import com.cisco.android.rum.integration.networkrequest.configurer.NetworkRequestConfigurer;

public class HttpConfigUtil {
    private static final String TAG = "HttpConfigUtil";

    private HttpConfigUtil() {
        throw new IllegalStateException("Class not meant to be instantiated");
    }

    public static boolean isNetworkTracingEnabled() {
        boolean isEnabled;
        try {
            isEnabled = NetworkRequestConfigurer.isNetworkTracingEnabled;
        } catch (Exception e) {
            //set value to the default (true) in case fetching from server fails.
            isEnabled = true;
            Log.d(TAG, "Network request module/remote configuration fetch failed.", e);
        }
        return isEnabled;
    }
}
