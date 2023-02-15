package com.splunk.rum;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import io.opentelemetry.rum.internal.DefaultingActivityLifecycleCallbacks;

/**
 * Registers the RumFragmentLifecycleCallbacks when an activity is created.
 */
class RumFragmentActivityRegisterer {

    private RumFragmentActivityRegisterer(){}

    static Application.ActivityLifecycleCallbacks create(FragmentManager.FragmentLifecycleCallbacks fragmentCallbacks){
        return new DefaultingActivityLifecycleCallbacks() {
            @Override
            public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity instanceof FragmentActivity) {
                    register((FragmentActivity) activity, fragmentCallbacks);
                }
            }
        };
    }

    static Application.ActivityLifecycleCallbacks createPre29(FragmentManager.FragmentLifecycleCallbacks fragmentCallbacks){
        return new DefaultingActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity instanceof FragmentActivity) {
                    register((FragmentActivity) activity, fragmentCallbacks);
                }
            }
        };
    }

    private static void register(FragmentActivity activity, FragmentManager.FragmentLifecycleCallbacks fragmentCallbacks) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        fragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true);
    }
}
