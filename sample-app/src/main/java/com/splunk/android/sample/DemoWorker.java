package com.splunk.android.sample;

import static com.splunk.android.sample.SampleApplication.INITIALIZE_RUM_KEY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DemoWorker extends Worker {

    private Context context;
    public static final String TAG = "DemoWorker";

    public DemoWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            startBackgroundService();
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(context, SplunkBackgroundService.class);
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SampleApplication.PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(INITIALIZE_RUM_KEY, false);
        editor.apply();
        Log.d(TAG, "Starting background Service");
        context.startService(serviceIntent);
    }
}