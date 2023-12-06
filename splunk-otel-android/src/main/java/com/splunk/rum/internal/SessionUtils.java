/*
 * Copyright Splunk Inc.
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

package com.splunk.rum.internal;

import android.util.Log;

public class SessionUtils {

    /** Performs an unsigned 32-bit conversion of the hex session id to a long. */
    static long convertToUInt32(String sessionId) {
        long acc = 0L;
        for (int i = 0; i < sessionId.length(); i += 8) {
            long chunk = 0;
            try {
                String chunkString = sessionId.substring(i, i + 8);
                chunk = Long.parseUnsignedLong(chunkString, 16);
            } catch (NumberFormatException e) {
                Log.w("SplunkRum", "Error parsing session id into long: " + sessionId);
            }
            acc = acc ^ chunk;
        }
        return acc;
    }
}
