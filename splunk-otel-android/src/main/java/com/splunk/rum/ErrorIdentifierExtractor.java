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

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ErrorIdentifierExtractor {

    private static final String SPLUNK_UUID_MANIFEST_KEY = "SPLUNK_O11Y_CUSTOM_UUID";
    private final Application application;
    private final PackageManager packageManager;
    @Nullable private final ApplicationInfo applicationInfo;

    public ErrorIdentifierExtractor(@NonNull Application application) {
        this.application = application;
        this.packageManager = application.getPackageManager();
        ApplicationInfo appInfo;
        try {
            appInfo =
                    packageManager.getApplicationInfo(
                            application.getPackageName(), PackageManager.GET_META_DATA);
        } catch (Exception e) {
            Log.e(
                    SplunkRum.LOG_TAG,
                    "Failed to initialize ErrorIdentifierExtractor: " + e.getMessage());
            appInfo = null;
        }
        this.applicationInfo = appInfo;
    }

    public ErrorIdentifierInfo extractInfo() {
        String applicationId = null;
        String versionCode = retrieveVersionCode();
        String customUUID = retrieveCustomUUID();

        if (applicationInfo != null) {
            applicationId = applicationInfo.packageName;
        } else {
            Log.e(SplunkRum.LOG_TAG, "ApplicationInfo is null, cannot extract applicationId");
        }

        return new ErrorIdentifierInfo(applicationId, versionCode, customUUID);
    }

    @Nullable
    private String retrieveVersionCode() {
        try {
            PackageInfo packageInfo =
                    packageManager.getPackageInfo(application.getPackageName(), 0);
            return String.valueOf(packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(SplunkRum.LOG_TAG, "Failed to get application version code", e);
            return null;
        }
    }

    @Nullable
    private String retrieveCustomUUID() {
        if (applicationInfo == null) {
            Log.e(SplunkRum.LOG_TAG, "ApplicationInfo is null; cannot retrieve Custom UUID.");
            return null;
        }
        Bundle bundle = applicationInfo.metaData;
        if (bundle != null) {
            return bundle.getString(SPLUNK_UUID_MANIFEST_KEY);
        } else {
            Log.e(SplunkRum.LOG_TAG, "Application MetaData bundle is null");
            return null;
        }
    }
}
