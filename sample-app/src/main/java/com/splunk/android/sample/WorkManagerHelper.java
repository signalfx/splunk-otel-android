package com.splunk.android.sample;

import android.content.Context;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

public class WorkManagerHelper {

    public static void startWorkManager(Context context) {
        WorkRequest demoWorkRequest =
                new OneTimeWorkRequest.Builder(DemoWorker.class)
                        .build();
        WorkManager
                .getInstance(context)
                .enqueue(demoWorkRequest);
    }

}