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

package com.cisco.android.rum.library.httpurlconnection;

import android.os.SystemClock;
import android.util.Log;

import com.cisco.android.rum.library.common.HttpConfigUtil;
import com.cisco.android.rum.library.httpurlconnection.tracing.HttpUrlConnectionSingletons;
import com.cisco.android.rum.library.httpurlconnection.tracing.RequestPropertySetter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.context.Context;

public class HttpUrlReplacements {

    static final Map<URLConnection, HttpURLConnectionInfo> activeURLConnections;
    private static final String TAG;
    public static final int UNKNOWN_RESPONSE_CODE = -1;

    static {
        activeURLConnections = new ConcurrentHashMap<>();
        TAG = "HttpUrlReplacements";
    }

    public static void replacementForDisconnect(HttpURLConnection c) {
        //Ensure ending of un-ended spans while connection is still alive
        //If disconnect is not called, harvester thread takes care of ending any un-ended spans.
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            reportWithResponseCode(c);
        }

        c.disconnect();
    }

    public static void replacementForConnect(URLConnection c) throws IOException {
        startTracingAtFirstConnection(c);

        try {
            c.connect();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        updateLastSeenTime(c);
        // connect() does not read anything from connection so request not harvestable yet (to be
        // reported if left idle).
    }

    public static Object replacementForContent(URLConnection c) throws IOException {
        return replaceThrowable(c, c::getContent);
    }

    public static Object replacementForContent(URLConnection c, Class<?>[] classes)
            throws IOException {
        return replaceThrowable(c, () -> c.getContent(classes));
    }

    public static String replacementForContentType(URLConnection c) {
        return replace(c, () -> c.getContentType());
    }

    public static String replacementForContentEncoding(URLConnection c) {
        return replace(c, () -> c.getContentEncoding());
    }

    public static int replacementForContentLength(URLConnection c) {
        return replace(c, () -> c.getContentLength());
    }

    public static long replacementForContentLengthLong(URLConnection c) {
        return replace(c, () -> c.getContentLengthLong());
    }

    public static long replacementForExpiration(URLConnection c) {
        return replace(c, () -> c.getExpiration());
    }

    public static long replacementForDate(URLConnection c) {
        return replace(c, () -> c.getDate());
    }

    public static long replacementForLastModified(URLConnection c) {
        return replace(c, () -> c.getLastModified());
    }

    public static String replacementForHeaderField(URLConnection c, String name) {
        return replace(c, () -> c.getHeaderField(name));
    }

    public static Map<String, List<String>> replacementForHeaderFields(URLConnection c) {
        return replace(c, () -> c.getHeaderFields());
    }

    public static int replacementForHeaderFieldInt(URLConnection c, String name, int Default) {
        return replace(c, () -> c.getHeaderFieldInt(name, Default));
    }

    public static long replacementForHeaderFieldLong(URLConnection c, String name, long Default) {
        return replace(c, () -> c.getHeaderFieldLong(name, Default));
    }

    public static long replacementForHeaderFieldDate(URLConnection c, String name, long Default) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldDate method.
        return replace(c, () -> c.getHeaderFieldDate(name, Default));
    }

    public static long replacementForHttpHeaderFieldDate(
            HttpURLConnection c, String name, long Default) {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldDate
        // method.
        return replace(c, () -> c.getHeaderFieldDate(name, Default));
    }

    public static String replacementForHeaderFieldKey(URLConnection c, int n) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldKey method.
        return replace(c, () -> c.getHeaderFieldKey(n));
    }

    public static String replacementForHttpHeaderFieldKey(HttpURLConnection c, int n) {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldKey
        // method.
        return replace(c, () -> c.getHeaderFieldKey(n));
    }

    public static String replacementForHeaderField(URLConnection c, int n) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderField method.
        return replace(c, () -> c.getHeaderField(n));
    }

    public static String replacementForHttpHeaderField(HttpURLConnection c, int n) {
        // URLConnection also overrides this and that is covered in replacementForHeaderField
        // method.
        return replace(c, () -> c.getHeaderField(n));
    }

    public static int replacementForResponseCode(URLConnection c) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) c;
        return replaceThrowable(c, httpURLConnection::getResponseCode);
    }

    public static String replacementForResponseMessage(URLConnection c) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) c;
        return replaceThrowable(c, httpURLConnection::getResponseMessage);
    }

    public static OutputStream replacementForOutputStream(URLConnection c) throws IOException {
        return replaceThrowable(c, c::getOutputStream, false);
    }

    public static InputStream replacementForInputStream(URLConnection c) throws IOException {
        startTracingAtFirstConnection(c);

        InputStream inputStream;
        try {
            inputStream = c.getInputStream();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        if (inputStream == null) {
            return inputStream;
        }

        return getInstrumentedInputStream(c, inputStream);
    }

    public static InputStream replacementForErrorStream(HttpURLConnection c) {
        startTracingAtFirstConnection(c);

        InputStream errorStream = c.getErrorStream();

        if (errorStream == null) {
            return errorStream;
        }

        return getInstrumentedInputStream(c, errorStream);
    }

    static InputStream getInstrumentedInputStream(
            URLConnection c, InputStream inputStream) {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                int res;
                try {
                    res = inputStream.read();
                } catch (IOException e) {
                    reportWithThrowable(c, e);
                    throw e;
                }
                reportIfDoneOrMarkHarvestable(res);
                return res;
            }

            @Override
            public int read(byte[] b) throws IOException {
                int res;
                try {
                    res = inputStream.read(b);
                } catch (IOException e) {
                    reportWithThrowable(c, e);
                    throw e;
                }
                reportIfDoneOrMarkHarvestable(res);
                return res;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int res;
                try {
                    res = inputStream.read(b, off, len);
                } catch (IOException e) {
                    reportWithThrowable(c, e);
                    throw e;
                }
                reportIfDoneOrMarkHarvestable(res);
                return res;
            }

            @Override
            public void close() throws IOException {
                HttpURLConnection httpURLConnection = (HttpURLConnection) c;
                reportWithResponseCode(httpURLConnection);
                inputStream.close();
            }

            void reportIfDoneOrMarkHarvestable(int result) {
                if (result == -1) {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) c;
                    reportWithResponseCode(httpURLConnection);
                } else {
                    markHarvestable(c);
                }
            }
        };
    }

    static <T> T replace(URLConnection c, ResultProvider<T> resultProvider) {
        startTracingAtFirstConnection(c);

        T result = resultProvider.get();

        updateLastSeenTime(c);
        markHarvestable(c);

        return result;
    }

    static <T> T replaceThrowable(
            URLConnection c, ThrowableResultProvider<T> resultProvider) throws IOException {
        return replaceThrowable(c, resultProvider, true);
    }

    static <T> T replaceThrowable(
            URLConnection c,
            ThrowableResultProvider<T> resultProvider,
            boolean shouldMarkHarvestable)
            throws IOException {
        startTracingAtFirstConnection(c);

        T result;
        try {
            result = resultProvider.get();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        updateLastSeenTime(c);
        if (shouldMarkHarvestable) {
            markHarvestable(c);
        }

        return result;
    }

    interface ResultProvider<T> {
        T get();
    }

    interface ThrowableResultProvider<T> {
        T get() throws IOException;
    }

    static void reportWithThrowable(URLConnection c, IOException e) {
        endTracing(c, UNKNOWN_RESPONSE_CODE, e);
    }

    static void reportWithResponseCode(HttpURLConnection c) {
        try {
            endTracing(c, c.getResponseCode(), null);
        } catch (IOException e) {
            Log.d(TAG, "An exception was thrown while ending span for connection " + c, e);
        }
    }

    static void endTracing(
            URLConnection c, int responseCode, Throwable error) {
        HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            Context context = info.context;
            HttpUrlConnectionSingletons.instrumenter().end(context, c, responseCode, error);
            info.reported = true;
            activeURLConnections.remove(c);
        }
    }

    static void startTracingAtFirstConnection(URLConnection c) {

        if (!HttpConfigUtil.isNetworkTracingEnabled()) {
            Log.d(TAG, "Network tracing has been disabled.");
            return;
        }

        if ((HttpUrlConnectionSingletons.instrumenter() == null)) {
            Log.d(TAG, "Instrumenter is null.");
            return;
        }

        Context parentContext = Context.current();
        if (!HttpUrlConnectionSingletons.instrumenter().shouldStart(parentContext, c)) {
            return;
        }

        if (!activeURLConnections.containsKey(c)) {
            Context context = HttpUrlConnectionSingletons.instrumenter().start(parentContext, c);
            activeURLConnections.put(c, new HttpURLConnectionInfo(context));
            try {
                injectContextToRequest(c, context);
            } catch (Exception e) {
                // If connection was already made prior to setting this request property,
                // (which should not happen as we've instrumented all methods that connect)
                // above call would throw IllegalStateException.
                Log.d(TAG, "An exception was thrown while adding distributed tracing context for connection "
                        + c.toString(), e);
            }
        }
    }

    static void injectContextToRequest(URLConnection connection, Context context) {
        HttpUrlConnectionSingletons.openTelemetrySdkInstance().getPropagators()
                .getTextMapPropagator()
                .inject(context, connection, RequestPropertySetter.INSTANCE);
    }

    static void updateLastSeenTime(URLConnection c) {
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            info.lastSeenTime = SystemClock.uptimeMillis();
        }
    }

    static void markHarvestable(URLConnection c) {
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            info.harvestable = true;
        }
    }

    static void reportIdleConnectionsOlderThan(long timeInterval) {
        final long timeNow = SystemClock.uptimeMillis();
        for (URLConnection c : activeURLConnections.keySet()) {
            final HttpURLConnectionInfo info = activeURLConnections.get(c);
            if (info != null
                    && info.harvestable
                    && !info.reported
                    && (info.lastSeenTime + timeInterval) < timeNow) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) c;
                reportWithResponseCode(httpURLConnection);
            }
        }
    }

    static class HttpURLConnectionInfo {
        long lastSeenTime;
        boolean reported;
        boolean harvestable;
        private Context context;

        HttpURLConnectionInfo(Context context) {
            this.context = context;
            lastSeenTime = SystemClock.uptimeMillis();
        }
    }
}

