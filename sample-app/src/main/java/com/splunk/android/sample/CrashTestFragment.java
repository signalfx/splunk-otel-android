package com.splunk.android.sample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crash_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button chainedExceptionButton = view.findViewById(R.id.button_chained_exception);
        chainedExceptionButton.setOnClickListener(v -> triggerChainedException());

        Button syntheticCodeButton = view.findViewById(R.id.button_synthetic_code);
        syntheticCodeButton.setOnClickListener(v -> triggerSyntheticCodeException());

        Button raceConditionButton = view.findViewById(R.id.button_race_condition);
        raceConditionButton.setOnClickListener(v -> triggerRaceConditionCrash());

        Button anrButton = view.findViewById(R.id.button_anr);
        anrButton.setOnClickListener(v -> triggerANR());

        Button navigateButton = view.findViewById(R.id.button_to_fragment_b);
        navigateButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(CrashTestFragment.this)
                    .navigate(R.id.action_CrashTestFragment_to_CrashTestFragmentB);
        });
    }

    private void triggerChainedException() {
        try {
            try {
                try {
                    // Deepest level: cause IndexOutOfBoundsException
                    List<String> list = new ArrayList<>();
                    String item = list.get(10);
                } catch (IndexOutOfBoundsException e) {
                    // Intermediate level: wrap in IOException
                    throw new IOException("Error accessing data", e);
                }
            } catch (IOException e) {
                // Top level: wrap in RuntimeException
                throw new RuntimeException("Operation failed", e);
            }
        } catch (RuntimeException e) {
            // Re-throw with another layer for deeper chaining
            throw new IllegalStateException("Application error occurred", e);
        }
    }

    // 2. Uses Java 8 lambdas which generate synthetic methods in bytecode
    private void triggerSyntheticCodeException() {
        // Define multiple lambdas to generate several synthetic methods
        Callable<String> callable1 = () -> {
            Callable<Integer> callable2 = () -> {
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

        // Multiple threads accessing the same list without synchronization
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            new Thread(() -> {
                try {
                    // Wait for all threads to be ready
                    startSignal.await();

                    // Perform competing operations on the shared list
                    for (int j = 0; j < 1000; j++) {
                        sharedList.add("Thread " + threadNum + " value " + j);
                        if (!sharedList.isEmpty()) {
                            // Should cause ConcurrentModificationException
                            // when another thread modifies the list at same time
                            sharedList.remove(0);
                        }
                    }
                } catch (Exception e) {
                    // Wrap the exception
                    throw new RuntimeException("Thread " + threadNum + " crashed", e);
                }
            }, "CrashTest-Thread-" + i).start();
        }

        // Start all threads at once
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