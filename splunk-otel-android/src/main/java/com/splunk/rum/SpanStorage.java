package com.splunk.rum;

import java.io.File;
import java.util.stream.Stream;

interface SpanStorage {

    File provideSpanFile();

    Stream<File> getAllSpanFiles();

    long getTotalFileSizeInBytes();

    Stream<File> getPendingFiles();
}
