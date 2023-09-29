package com.splunk.rum;

import java.io.File;
import java.util.stream.Stream;

/***
 * Manage location for storing span, provide spans that can be sent to exporter
 * or all spans stored in the location.
 */
interface SpanStorage {

    /***
     * Provide location of storing spans
     * @return
     */
    File provideSpanFile();

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
