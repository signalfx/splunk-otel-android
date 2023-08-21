package com.splunk.rum;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Build;
import java.lang.reflect.Method;

public class BackgroundProcessDetector {
    public static Boolean isBackgroundProcess(String applicationId) {
        String applicationProcessName = "";
        if (Build.VERSION.SDK_INT >= 28)
            applicationProcessName = Application.getProcessName();
        else {
            try {
                @SuppressLint("PrivateApi")
                Class<?> activityThread = Class.forName("android.app.ActivityThread");
                String methodName = "currentProcessName";
                @SuppressLint("PrivateApi") Method getProcessName = activityThread.getDeclaredMethod(methodName);
                applicationProcessName = (String) getProcessName.invoke(null);
            } catch (Exception e) {
            }
        }

        return applicationProcessName != null && applicationProcessName.equals(applicationId);
    }
}
