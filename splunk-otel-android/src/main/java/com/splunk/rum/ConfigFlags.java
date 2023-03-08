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

public class ConfigFlags {
    private boolean debugEnabled = false;
    private boolean diskBufferingEnabled = false;
    private boolean reactNativeSupportEnabled = false;
    private boolean crashReportingEnabled = true;
    private boolean networkMonitorEnabled = true;
    private boolean anrDetectionEnabled = true;
    private boolean slowRenderingDetectionEnabled = true;

    public void enableDebug() {
        debugEnabled = true;
    }

    public void enableDiskBuffering() {
        diskBufferingEnabled = true;
    }

    public void enableReactNativeSupport() {
        reactNativeSupportEnabled = true;
    }

    public void disableCrashReporting() {
        crashReportingEnabled = false;
    }

    public void disableNetworkMonitor() {
        networkMonitorEnabled = false;
    }

    public void disableAnrDetection() {
        anrDetectionEnabled = false;
    }

    public void disableSlowRenderingDetection() {
        slowRenderingDetectionEnabled = false;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isAnrDetectionEnabled() {
        return anrDetectionEnabled;
    }

    public boolean isNetworkMonitorEnabled() {
        return networkMonitorEnabled;
    }

    public boolean isSlowRenderingDetectionEnabled() {
        return slowRenderingDetectionEnabled;
    }

    public boolean isCrashReportingEnabled() {
        return crashReportingEnabled;
    }

    public boolean isDiskBufferingEnabled() {
        return diskBufferingEnabled;
    }

    public boolean isReactNativeSupportEnabled() {
        return reactNativeSupportEnabled;
    }
}
