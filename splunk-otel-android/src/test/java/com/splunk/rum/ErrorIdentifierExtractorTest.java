package com.splunk.rum;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ErrorIdentifierExtractorTest {
    private static final String SPLUNK_UUID_MANIFEST_KEY = "SPLUNK_OLLY_CUSTOM_UUID";

    @Mock
    private Application mockApplication;

    @Mock
    private PackageManager mockPackageManager;

    @Mock
    private PackageInfo mockPackageInfo;

    @Mock
    private ApplicationInfo mockApplicationInfo;

    private ErrorIdentifierExtractor extractor;

    @Mock
    private Bundle mockMetadata;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(mockApplication.getApplicationContext()).thenReturn(mockApplication);
        when(mockApplication.getPackageManager()).thenReturn(mockPackageManager);
        when(mockApplication.getPackageName()).thenReturn("com.splunk.test");

        mockApplicationInfo.packageName = "com.splunk.test";
        mockApplicationInfo.metaData = mockMetadata;

        when(mockPackageManager.getApplicationInfo("com.splunk.test", PackageManager.GET_META_DATA))
                .thenReturn(mockApplicationInfo);
        when(mockMetadata.getString(SPLUNK_UUID_MANIFEST_KEY)).thenReturn("test-uuid");

        mockPackageInfo.versionCode = 123;
        when(mockPackageManager.getPackageInfo("com.splunk.test", 0)).thenReturn(mockPackageInfo);

        extractor = ErrorIdentifierExtractor.getInstance(mockApplication);
    }


    @Test
    public void testGetApplicationId() {
        assertEquals("com.splunk.test", extractor.getApplicationId());
    }

    @Test
    public void testGetVersionCode() {
        assertEquals("123", extractor.getVersionCode());
    }

    @Test
    public void testGetCustomUUID() {
        extractor = ErrorIdentifierExtractor.getInstance(mockApplication);

        assert extractor != null;
        assertEquals("test-uuid", extractor.getCustomUUID());
    }
}