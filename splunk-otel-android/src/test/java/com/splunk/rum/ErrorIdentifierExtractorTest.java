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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ErrorIdentifierExtractorTest {
    private static final String SPLUNK_UUID_MANIFEST_KEY = "splunk.build_id";
    private static final String TEST_PACKAGE_NAME = "splunk.test.package.name";
    private static final String TEST_VERSION_CODE = "123";
    private static final String TEST_UUID = "test-uuid";

    @Mock private Application mockApplication;
    @Mock private PackageManager mockPackageManager;
    @Mock private PackageInfo mockPackageInfo;
    @Mock private ApplicationInfo mockApplicationInfo;
    @Mock private Bundle mockMetadata;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(mockApplication.getApplicationContext()).thenReturn(mockApplication);
        when(mockApplication.getPackageManager()).thenReturn(mockPackageManager);
        when(mockApplication.getPackageName()).thenReturn(TEST_PACKAGE_NAME);

        mockApplicationInfo.packageName = TEST_PACKAGE_NAME;
        mockApplicationInfo.metaData = mockMetadata;

        when(mockPackageManager.getApplicationInfo(TEST_PACKAGE_NAME, PackageManager.GET_META_DATA))
                .thenReturn(mockApplicationInfo);
        when(mockMetadata.getString(SPLUNK_UUID_MANIFEST_KEY)).thenReturn(TEST_UUID);

        mockPackageInfo.versionCode = 123;
        when(mockPackageManager.getPackageInfo(TEST_PACKAGE_NAME, 0)).thenReturn(mockPackageInfo);
    }

    @Test
    public void testGetApplicationId() {
        ErrorIdentifierExtractor extractor = new ErrorIdentifierExtractor(mockApplication);
        assertEquals(TEST_PACKAGE_NAME, extractor.extractInfo().getApplicationId());
    }

    @Test
    public void testGetVersionCode() {
        ErrorIdentifierExtractor extractor = new ErrorIdentifierExtractor(mockApplication);
        assertEquals(TEST_VERSION_CODE, extractor.extractInfo().getVersionCode());
    }

    @Test
    public void testGetCustomUUID() {
        ErrorIdentifierExtractor extractor = new ErrorIdentifierExtractor(mockApplication);
        assertEquals(TEST_UUID, extractor.extractInfo().getCustomUUID());
    }

    @Test
    public void testCustomUUIDButDoesNotExist() {
        when(mockMetadata.getString(SPLUNK_UUID_MANIFEST_KEY)).thenReturn(null);
        ErrorIdentifierExtractor extractor = new ErrorIdentifierExtractor(mockApplication);
        assertNull(extractor.extractInfo().getCustomUUID());
    }

    @Test
    public void testApplicationInfoMetaDataIsNull() throws PackageManager.NameNotFoundException {
        ApplicationInfo applicationInfoWithNullMetaData = new ApplicationInfo();
        applicationInfoWithNullMetaData.packageName = TEST_PACKAGE_NAME;

        when(mockPackageManager.getApplicationInfo(TEST_PACKAGE_NAME, PackageManager.GET_META_DATA))
                .thenReturn(applicationInfoWithNullMetaData);

        ErrorIdentifierExtractor extractor = new ErrorIdentifierExtractor(mockApplication);
        assertNull(extractor.extractInfo().getCustomUUID());
    }

    @Test
    public void testRetrieveVersionCodeIsNull() throws PackageManager.NameNotFoundException {
        when(mockPackageManager.getPackageInfo(TEST_PACKAGE_NAME, 0))
                .thenThrow(new PackageManager.NameNotFoundException());

        ErrorIdentifierExtractor extractor = new ErrorIdentifierExtractor(mockApplication);
        assertNull(extractor.extractInfo().getVersionCode());
    }

    @Test
    public void testExtractInfoWhenApplicationInfoIsNull()
            throws PackageManager.NameNotFoundException {
        when(mockPackageManager.getApplicationInfo(TEST_PACKAGE_NAME, PackageManager.GET_META_DATA))
                .thenThrow(new PackageManager.NameNotFoundException());

        ErrorIdentifierExtractor extractor = new ErrorIdentifierExtractor(mockApplication);

        ErrorIdentifierInfo info = extractor.extractInfo();
        assertNull(info.getApplicationId());
        assertEquals(TEST_VERSION_CODE, info.getVersionCode());
        assertNull(info.getCustomUUID());
    }
}
