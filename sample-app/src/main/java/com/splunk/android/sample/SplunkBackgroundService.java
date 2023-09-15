package com.splunk.android.sample;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SplunkBackgroundService extends Service {
    public static final String TAG = "SplunkBackgroundService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.d(TAG, "Service started on different thread");
        }
        stopSelf();
        return START_STICKY;
    }
}
