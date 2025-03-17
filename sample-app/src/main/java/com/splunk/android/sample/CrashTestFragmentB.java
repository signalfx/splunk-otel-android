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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CrashTestFragmentB extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crash_test_b, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button inliningButton = view.findViewById(R.id.button_inlining);
        inliningButton.setOnClickListener(v -> triggerInliningException());

        Button reflectionButton = view.findViewById(R.id.button_reflection);
        reflectionButton.setOnClickListener(v -> triggerReflectionException());

        Button inheritanceButton = view.findViewById(R.id.button_inheritance);
        inheritanceButton.setOnClickListener(v -> triggerInheritanceException());

        Button oomButton = view.findViewById(R.id.button_oom);
        oomButton.setOnClickListener(v -> triggerOutOfMemoryError());

        Button overloadButton = view.findViewById(R.id.button_overload);
        overloadButton.setOnClickListener(v -> triggerOverloadedMethodCrash());
    }

    private void triggerInliningException() {
        int a = inlinableMethod1(5);
        int b = inlinableMethod2(0);
        int result = a / b; // Division by zero
    }

    private int inlinableMethod1(int value) {
        return value * 5;
    }

    private int inlinableMethod2(int value) {
        return value;
    }

    private void triggerReflectionException() {
        try {
            Method method = String.class.getMethod("nonExistentMethod");

            // won't reach here
            method.invoke("test");
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed", e);
        }
    }

    private void triggerInheritanceException() {
        DeepestChild deepChild = new DeepestChild();

        deepChild.methodThatThrows();
    }

    private static class BaseClass {
        protected void baseMethod() {
            throw new RuntimeException("Exception in base class");
        }
    }

    private static class FirstChild extends BaseClass {
        @Override
        protected void baseMethod() {
            try {
                super.baseMethod();
            } catch (RuntimeException e) {
                throw new IllegalStateException("Exception in first child", e);
            }
        }
    }

    private static class SecondChild extends FirstChild {
        protected void midMethod() {
            baseMethod();
        }
    }

    private static class DeepestChild extends SecondChild {
        public void methodThatThrows() {
            midMethod();
        }
    }

    private void triggerOutOfMemoryError() {
        try {
            ArrayList<byte[]> memoryHog = new ArrayList<>();

            // Allocating memory in chunks until out of memory
            for (int i = 0; i < 100; i++) {
                memoryHog.add(new byte[10 * 1024 * 1024]); // 10MB chunks
            }
        } catch (OutOfMemoryError e) {
            throw e;
        }
    }

    private void triggerOverloadedMethodCrash() {
        crashingMethod("test");
    }

    private void crashingMethod(String param) {
        crashingMethod(param, 10);
    }

    private void crashingMethod(String param, int value) {
        crashingMethod(param, value, true);
    }

    private void crashingMethod(String param, int value, boolean flag) {
        crashingMethod(param, value, flag, new HashMap<>());
    }

    private void crashingMethod(String param, int value, boolean flag, Map<String, Object> extra) {
        if (param != null && value > 0 && flag) {
            throw new IllegalArgumentException(
                    "Crasher called with: " + param + ", " + value + ", " + flag);
        }
    }
}
