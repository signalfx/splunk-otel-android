package com.splunk.rum.internal;

import android.util.Log;

public class SessionUtils {

    /**
     * Performs an unsigned 32-bit conversion of the hex session id to a long.
     */
    static long convertToUInt32(String sessionId) {
        long acc = 0L;
        for (int i = 0; i < sessionId.length(); i += 8) {
            long chunk = 0;
            try {
                String chunkString = sessionId.substring(i, i+8);
                chunk = Long.parseUnsignedLong(chunkString, 16);
            } catch (NumberFormatException e) {
                Log.w("SplunkRum", "Error parsing session id into long: " + sessionId);
            }
            acc = acc ^ chunk;
        }
        return acc;
    }


}
