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
import androidx.navigation.fragment.NavHostFragment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class CrashTestFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crash_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button simpleCrashButton = view.findViewById(R.id.button_simple_crash);
        simpleCrashButton.setOnClickListener(v -> simpleCrash());

        Button chainedExceptionButton = view.findViewById(R.id.button_chained_exception);
        chainedExceptionButton.setOnClickListener(v -> triggerChainedException());

        Button syntheticCodeButton = view.findViewById(R.id.button_synthetic_code);
        syntheticCodeButton.setOnClickListener(v -> triggerSyntheticCodeException());

        Button raceConditionButton = view.findViewById(R.id.button_race_condition);
        raceConditionButton.setOnClickListener(v -> triggerRaceConditionCrash());

        Button anrButton = view.findViewById(R.id.button_anr);
        anrButton.setOnClickListener(v -> triggerANR());

        Button navigateButton = view.findViewById(R.id.button_to_fragment_b);
        navigateButton.setOnClickListener(
                v -> {
                    NavHostFragment.findNavController(CrashTestFragment.this)
                            .navigate(R.id.action_CrashTestFragment_to_CrashTestFragmentB);
                });
    }

    private void simpleCrash() {
        throw new RuntimeException("Simple RuntimeException Crash");
    }

    private void triggerChainedException() {
        try {
            try {
                try {
                    List<String> list = new ArrayList<>();
                    String item = list.get(10); // cause IndexOutOfBoundsException
                } catch (IndexOutOfBoundsException e) {
                    throw new IOException("Error accessing data", e);
                }
            } catch (IOException e) {
                throw new RuntimeException("Operation failed", e);
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("Application error occurred", e);
        }
    }

    private void triggerSyntheticCodeException() {
        Callable<String> callable1 =
                () -> {
                    Callable<Integer> callable2 =
                            () -> {
                                String nullStr = null;
                                return nullStr.length(); // cause NPE
                            };

                    try {
                        return "Result: " + callable2.call();
                    } catch (Exception e) {
                        throw new RuntimeException("Nested lambda failure", e);
                    }
                };

        try {
            callable1.call();
        } catch (Exception e) {
            throw new RuntimeException("Synthetic method crash", e);
        }
    }

    private void triggerRaceConditionCrash() {
        final List<String> sharedList = new ArrayList<>();
        final CountDownLatch startSignal = new CountDownLatch(1);
        final int threadCount = 5;

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            new Thread(
                            () -> {
                                try {
                                    startSignal.await();

                                    for (int j = 0; j < 1000; j++) {
                                        sharedList.add("Thread " + threadNum + " value " + j);
                                        if (!sharedList.isEmpty()) {
                                            sharedList.remove(0);
                                        }
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(
                                            "Thread " + threadNum + " crashed", e);
                                }
                            },
                            "CrashTest-Thread-" + i)
                    .start();
        }
        startSignal.countDown();
    }

    private void triggerANR() {
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
