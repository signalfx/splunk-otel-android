package com.splunk.rum;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

public class ErrorIdentifierExtractor {

    private static final String SPLUNK_UUID_MANIFEST_KEY = "SPLUNK_OLLY_CUSTOM_UUID";
    @Nullable
    private static ErrorIdentifierExtractor instance = null;

    private final Context context;
    private final PackageManager packageManager;
    private final ApplicationInfo applicationInfo;

    @Nullable
    private final String applicationId;
    @Nullable
    private final String versionCode;
    @Nullable
    private final String customUUID;

    private ErrorIdentifierExtractor(Application application) throws PackageManager.NameNotFoundException {
        this.context = application.getApplicationContext();
        this.packageManager = context.getPackageManager();
        this.applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

        // Initialize values
        this.applicationId = applicationInfo.packageName;
        this.versionCode = retrieveVersionCode();
        this.customUUID = retrieveCustomUUID();
    }

    @Nullable
    public static synchronized ErrorIdentifierExtractor getInstance(Application application) {
        if (instance == null) {
            try {
                instance = new ErrorIdentifierExtractor(application);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(SplunkRum.LOG_TAG, "Failed to initialize ErrorIdentifierExtractor: " + e.getMessage());
            }
        }
        return instance;
    }

    @Nullable
    public String getApplicationId() {
        return applicationId;
    }

    @Nullable
    public String getVersionCode() {
        return versionCode;
    }

    @Nullable
    public String getCustomUUID() {
        return customUUID;
    }

    @Nullable
    private String retrieveVersionCode() {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return String.valueOf(packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(SplunkRum.LOG_TAG, "Failed to get application version code", e);
            return null;
        }
    }

    @Nullable
    private String retrieveCustomUUID() {
        Bundle bundle = applicationInfo.metaData;

        if (bundle != null) {
            return bundle.getString(SPLUNK_UUID_MANIFEST_KEY);
        } else {
            Log.e(SplunkRum.LOG_TAG, "Application MetaData bundle is null");
            return null;
        }
    }
}
