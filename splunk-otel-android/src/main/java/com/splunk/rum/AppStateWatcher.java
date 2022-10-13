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

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

class AppStateWatcher implements Application.ActivityLifecycleCallbacks {

    private final List<AppStateListener> appStateListeners;
    // we count the number of activities that have been "started" and not yet "stopped" here to
    // figure out when the app goes into the background.
    private int numberOfOpenActivities = 0;

    AppStateWatcher(List<AppStateListener> appStateListeners) {
        this.appStateListeners = appStateListeners;
    }

    @Override
    public void onActivityCreated(
            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (numberOfOpenActivities == 0) {
            for (AppStateListener appListener : appStateListeners) {
                appListener.appForegrounded();
            }
        }
        numberOfOpenActivities++;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {}

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (--numberOfOpenActivities == 0) {
            for (AppStateListener appListener : appStateListeners) {
                appListener.appBackgrounded();
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
}
