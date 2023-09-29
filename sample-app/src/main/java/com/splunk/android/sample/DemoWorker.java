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

package com.splunk.android.sample;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.splunk.rum.SplunkRum;

import io.opentelemetry.api.common.Attributes;

public class DemoWorker extends Worker {

    private Context context;
    public static final String TAG = "SplunkRum";

    public DemoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SplunkRum.getInstance().addRumEvent("DemoWorker is doing work", Attributes.empty());
            Log.d(TAG, "DemoWorker Starting background Service");
            startBackgroundService();
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(context, SplunkBackgroundService.class);
        Log.d(TAG, "Starting background Service");
        context.startService(serviceIntent);
    }
}