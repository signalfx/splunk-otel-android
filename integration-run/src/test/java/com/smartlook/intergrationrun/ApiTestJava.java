/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.intergrationrun;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.splunk.android.core.api.Preferences;
import com.splunk.android.core.api.Sensitivity;
import com.splunk.android.core.api.Smartlook;
import com.splunk.android.core.api.User;
import com.splunk.android.core.api.enumeration.Status;
import com.splunk.android.core.api.model.Properties;
import com.splunk.android.core.api.model.RecordingMask;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class ApiTestJava {

    @Test
    public void testApiJava() {
        Smartlook smartlook = Smartlook.getInstance();
        Preferences preferences = smartlook.getPreferences();

        Integer frameRate = preferences.getFrameRate();
        preferences.setFrameRate(frameRate);

        Preferences.EventTracking.Navigation navigation = preferences.getEventTracking().getNavigation();

        Set<Class<? extends Activity>> disabledActivityClasses = navigation.getDisabledActivityClasses();
        disabledActivityClasses.add(DummyActivity.class);

        Set<Class<? extends Fragment>> disabledFragmentClasses = navigation.getDisabledFragmentClasses();
        disabledFragmentClasses.add(DummyFragment.class);

        Properties eventProperties = smartlook.getEventProperties();
        eventProperties.clear();
        eventProperties.getString("dummy");
        eventProperties.putString("dummy", "dummy");
        eventProperties.remove("Dummy");


        smartlook.getLog().setAllowedLogAspects(LogAspect.ALL);
        smartlook.getLog().getListeners().add(
                (l, s, s1, s2) -> {
                }
        );

        smartlook.setRecordingMask(new RecordingMask(Collections.emptyList()));

        smartlook.trackEvent("a");

        smartlook.trackEvent("a", new Properties()
                .putString("a", "b")
                .putString("c", "d"));

        Set<User.Listener> userListeners = smartlook.getUser().getListeners();
        userListeners.add(url -> System.out.println("blah"));

        Sensitivity sensitivity = smartlook.getSensitivity();

        Boolean classSensitivity = sensitivity.getViewClassSensitivity(DummyView.class);
        sensitivity.setViewClassSensitivity(DummyView.class, classSensitivity);

        DummyView view = new DummyView(RuntimeEnvironment.getApplication());
        Boolean viewSensitivity = sensitivity.getViewInstanceSensitivity(view);
        sensitivity.setViewInstanceSensitivity(view, viewSensitivity);

        Status status = smartlook.getState().getStatus();
    }

    private static class DummyActivity extends Activity {
    }

    private static class DummyFragment extends Fragment {
    }

    private static class DummyView extends View {
        public DummyView(Context context) {
            super(context);
        }
    }
}
