package com.splunk.rum;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ZipkinToDiskSenderTest {

    private final long now = System.currentTimeMillis();
    private final Path path = Paths.get("/my/great/storage/location");
    private final String tmpFile = "/my/great/storage/location/" + now + ".spans.tmp";
    private final Path tmpPath = Paths.get(tmpFile);
    private final String finalFile = "/my/great/storage/location/" + now + ".spans";
    private final Path finalPath = Paths.get(finalFile);
    private final byte[] span1 = "span one".getBytes(StandardCharsets.UTF_8);
    private final byte[] span2 = "span one".getBytes(StandardCharsets.UTF_8);

    @Mock
    private FileUtils fileUtils;
    @Mock
    private Clock clock;

    @Before
    public void setup(){
        when(clock.millis()).thenReturn(now);
    }

    @Test
    public void testHappyPath() throws Exception {
        List<byte[]> spans = Arrays.asList(span1, span2);

        ZipkinToDiskSender sender = new ZipkinToDiskSender(path, fileUtils, clock);
        sender.sendSpans(spans);

        verify(fileUtils).writeAsLines(tmpPath, spans);
        verify(fileUtils).moveAtomic(tmpPath, finalPath);
    }

    @Test
    public void testWriteFails() throws Exception {
        List<byte[]> spans = Arrays.asList(span1, span2);
        doThrow(new IOException("boom")).when(fileUtils).writeAsLines(tmpPath, spans);

        ZipkinToDiskSender sender = new ZipkinToDiskSender(path, fileUtils, clock);
        sender.sendSpans(spans);

        verify(fileUtils, never()).moveAtomic(any(), any());
    }

    @Test
    public void testRenameFails() throws Exception {
        List<byte[]> spans = Arrays.asList(span1, span2);
        doThrow(new IOException("boom")).when(fileUtils).moveAtomic(tmpPath, finalPath);

        ZipkinToDiskSender sender = new ZipkinToDiskSender(path, fileUtils, clock);
        sender.sendSpans(spans);
        verify(fileUtils).moveAtomic(tmpPath, finalPath);

    }

}