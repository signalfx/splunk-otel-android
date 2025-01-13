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

package com.splunk.rum.library.httpurlconnection;

/**
 * Configuration for automatic instrumentation of HttpURLConnection/HttpsURLConnection requests.
 */
public final class HttpUrlInstrumentationConfig {

    // Time (ms) to wait before assuming that an idle connection is no longer
    // in use and should be reported.
    private static final long CONNECTION_INACTIVITY_TIMEOUT = 10000;

    private HttpUrlInstrumentationConfig() {
    }

    /**
     * Returns a runnable that can be scheduled to run periodically at a fixed interval to close
     * open spans if connection is left idle for CONNECTION_INACTIVITY_TIMEOUT duration. Runnable
     * interval is same as CONNECTION_INACTIVITY_TIMEOUT. CONNECTION_INACTIVITY_TIMEOUT in milli
     * seconds can be obtained from getReportIdleConnectionInterval() API.
     *
     * @return The idle connection reporting runnable
     */
    public static Runnable getReportIdleConnectionRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                HttpUrlReplacements.reportIdleConnectionsOlderThan(
                        CONNECTION_INACTIVITY_TIMEOUT);
            }

            @Override
            public String toString() {
                return "ReportIdleConnectionsRunnable";
            }
        };
    }

    /**
     * The fixed interval duration in milli seconds that the runnable from
     * getReportIdleConnectionRunnable() API should be scheduled to periodically run at.
     *
     * @return The fixed interval duration in ms
     */
    public static long getReportIdleConnectionInterval() {
        return CONNECTION_INACTIVITY_TIMEOUT;
    }
}

