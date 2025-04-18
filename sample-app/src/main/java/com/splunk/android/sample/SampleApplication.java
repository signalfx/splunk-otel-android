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

package com.splunk.android.sample;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import com.splunk.rum.SplunkRum;
import com.splunk.rum.StandardAttributes;
import io.opentelemetry.api.common.Attributes;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.regex.Pattern;
import okhttp3.Request;

public class SampleApplication extends Application {

    private static final Pattern HTTP_URL_SENSITIVE_DATA_PATTERN =
            Pattern.compile("(user|pass)=\\w+");

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        new Thread(() -> {
            try {
                URL url = new URL("https://rum-ingest.lab0.signalfx.com");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                Log.d("CONNECTION_TEST", "Response code: " + responseCode);
            } catch (Exception e) {
                Log.e("CONNECTION_TEST", "Error connecting: " + e.getMessage());
            }
        }).start();

        // Add this to your activity for testing
        try {
            InetAddress address = InetAddress.getByName("google.com");
            Log.d("NETWORK_TEST!!!", "Successfully resolved google.com: " + address.getHostAddress());
        } catch (Exception e) {
            Log.e("NETWORK_TEST!", "Failed to resolve google.com: " + Arrays.toString(e.getStackTrace()));
        }
        try {
            InetAddress address = InetAddress.getByName("rum-ingest.lab0.signalfx.com");
            Log.d("NETWORK_TEST!", "Successfully resolved SignalFx endpoint: " + address.getHostAddress());
        } catch (Exception e) {
            Log.e("NETWORK_TEST!", "Failed to resolve SignalFx endpoint: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
        }

        try {
            InetAddress address = InetAddress.getByName("rum-ingest.us0.signalfx.com");
            Log.d("NETWORK_TEST!", "Successfully resolved SignalFx endpoint: " + address.getHostAddress());
        } catch (Exception e) {
            Log.e("NETWORK_TEST!", "Failed to resolve SignalFx endpoint: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
        }

        SplunkRum.builder()
                // note: for these values to be resolved, put them in your local.properties
                // file as rum.beacon.url and rum.access.token
//                .setRealm(getResources().getString(R.string.rum_realm))
                .setRealm("lab0")
                .setApplicationName("fridayBugBash!123")
                .setRumAccessToken("nollRTbzroTXX9kjiufv6A") // sf monitoring
//                .setRumAccessToken("cXxDmRIr5e-UyJfz3t27pg") //  app d org
//                .setRumAccessToken("a5OVE71NXiRkCYxH34_Fbw") //us0
                .enableDebug()
                .enableDiskBuffering()
                .disableSubprocessInstrumentation(BuildConfig.APPLICATION_ID)
                .enableBackgroundInstrumentationDeferredUntilForeground()
                .setSlowRenderingDetectionPollInterval(Duration.ofMillis(1000))
                .setDeploymentEnvironment("test")
                .limitDiskUsageMegabytes(1)
                .setGlobalAttributes(
                        Attributes.builder()
                                .put("vendor", "Splunk")
                                .put(StandardAttributes.APP_VERSION, BuildConfig.VERSION_NAME)
                                .build())
                .filterSpans(
                        spanFilter ->
                                spanFilter
                                        .removeSpanAttribute(stringKey("http.user_agent"))
                                        .rejectSpansByName(spanName -> spanName.contains("ignored"))
                                        // sensitive data in the login http.url attribute
                                        // will be redacted before it hits the exporter
                                        .replaceSpanAttribute(
                                                StandardAttributes.HTTP_URL,
                                                value ->
                                                        HTTP_URL_SENSITIVE_DATA_PATTERN
                                                                .matcher(value)
                                                                .replaceAll("$1=<redacted>")))
                .setHttpSenderCustomizer(
                        okHttpBuilder -> {
                            okHttpBuilder.compressionEnabled(true);
                            okHttpBuilder
                                    .clientBuilder()
                                    .addInterceptor(
                                            chain -> {
                                                Request.Builder requestBuilder =
                                                        chain.request().newBuilder();
                                                requestBuilder.header(
                                                        "X-My-Custom-Header", "abc123");
                                                return chain.proceed(requestBuilder.build());
                                            });
                        })
                .build(this);
    }
}
