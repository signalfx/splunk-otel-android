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

package com.splunk.rum.incubating;

import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * This interface can be used to customize the exporter used to send telemetry to Splunk. It is not
 * yet stable and its APIs are subject to change at any time.
 *
 * @since 1.4.0
 */
public interface HttpSenderCustomizer {

    HttpSenderCustomizer DEFAULT = x -> {};

    void customize(OkHttpSender.Builder builder);
}
