package com.splunk.rum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;

public class StartTypeAwareSpanStorageTest {

    private final FileUtils fileUtils = mock();

    private final File rootDir = new File("files/");
    private final VisibleScreenTracker visibleScreenTracker = mock();

    private StartTypeAwareSpanStorage fileProvider;

    @BeforeEach
    void setup(){
        when(fileUtils.getSpansDirectory(rootDir)).thenReturn(new File(rootDir, "spans"));
        fileProvider = new StartTypeAwareSpanStorage(visibleScreenTracker, fileUtils, rootDir);
    }

    @Test
    void getPendingFiles_givenInBackground_shouldReturnForegoundOnlySpan(){
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);
        List<File> spans = fileProvider.getPendingFiles().collect(Collectors.toList());
        assertEquals(0, spans.size());
    }

    @Test
    void getPendingFiles_givenPrevouslyInBackground_shouldMoveBackgroundSpanToForegroundSpanForSending(){
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("MainActivity");

        List<File> backgroundFiles = new ArrayList<>();
        File fileToMove = mock();
        ArgumentCaptor<File> fileDestinationCaptor = ArgumentCaptor.forClass(File.class);
        when(fileToMove.renameTo(fileDestinationCaptor.capture())).thenReturn(true);
        when(fileToMove.getName()).thenReturn("tosend.span");
        backgroundFiles.add(fileToMove);

        ArgumentCaptor<File> fileSourceCaptor = ArgumentCaptor.forClass(File.class);
        when(fileUtils.listSpanFiles(fileSourceCaptor.capture())).thenReturn(backgroundFiles.stream(), backgroundFiles.stream());

        List<File> spans = fileProvider.getPendingFiles().collect(Collectors.toList());

        verify(fileToMove).renameTo(any());

        String destinationPath = fileDestinationCaptor.getValue().getPath();
        assertEquals("files/spans/tosend.span", destinationPath);

        String sourcePath = fileSourceCaptor.getAllValues().get(0).getPath();
        assertTrue(sourcePath.startsWith("files/spans/background"));
        assertEquals(backgroundFiles.size(), spans.size());
    }

    @Test
    void getSpanPath_givenInBackground_shouldReturnBackgroundSpanPath(){
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);

        File path = fileProvider.provideSpanFile();

        assertTrue(path.getPath().startsWith("files/spans/background/"));
    }

    @Test
    void getSpanPath_givenInForeground_shouldReturnForegroundSpanPath(){
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("MainActivity");

        File path = fileProvider.provideSpanFile();

        assertFalse(path.getPath().startsWith("files/spans/background/"));
    }

}