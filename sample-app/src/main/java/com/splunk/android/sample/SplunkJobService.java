package com.splunk.android.sample;


import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


@SuppressLint("SpecifyJobSchedulerIdRange")
public class SplunkJobService extends JobService {
    private String TAG = SplunkJobService.class.getSimpleName();

    private Context context;

    @Override
    public boolean onStartJob(JobParameters params) {
//        Intent serviceIntent = new Intent(context, SplunkBackgroundService.class);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
//        Intent serviceIntent = new Intent(context, SplunkBackgroundService.class);
//        context.stopService(serviceIntent);
        Log.d(TAG, "Job paused.");
        return true;
    }

}
