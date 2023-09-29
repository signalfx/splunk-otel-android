package com.splunk.rum;

import java.io.File;
import java.util.stream.Stream;

interface SpanFileProvider {

    File provideSpanPath();

    Stream<File> getAllSpanFiles();

    long getTotalFileSizeInBytes();

    Stream<File> getPendingFiles();
}
