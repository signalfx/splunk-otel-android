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

package com.splunk.rum;

import java.io.File;
import java.util.stream.Stream;

/***
 * Manage location for storing span, provide spans that can be sent to exporter
 * or all spans stored in the location.
 */
interface SpanStorage {

    /***
     * Returns the location where spans are buffered.
     */
    File provideSpansDirectory();

    /***
     * @return all spans including those that can be sent or not
     */
    Stream<File> getAllSpanFiles();

    /***
     * @return total size of all span that can be sent or not
     */
    long getTotalFileSizeInBytes();

    /***
     * @return all spans that can be sent
     */
    Stream<File> getPendingFiles();
}
