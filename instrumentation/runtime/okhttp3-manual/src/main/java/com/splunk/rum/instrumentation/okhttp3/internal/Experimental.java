/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.rum.instrumentation.okhttp3.internal;

import android.annotation.SuppressLint;
import androidx.annotation.Nullable;
import com.splunk.rum.instrumentation.okhttp3.OkHttpTelemetryBuilder;
import java.util.function.BiConsumer;

/**
 * This class is internal and experimental. Its APIs are unstable and can change at any time. Its
 * APIs (or a version of them) may be promoted to the public stable API in the future, but no
 * guarantees are made.
 */
public final class Experimental {

  @Nullable
  private static volatile BiConsumer<OkHttpTelemetryBuilder, Boolean> setEmitExperimentalTelemetry;

  /**
   * Sets whether experimental HTTP telemetry should be emitted.
   *
   * @param builder the telemetry builder
   * @param emitExperimentalTelemetry {@code true} to emit experimental telemetry
   */
  @SuppressLint("NewApi") // Requires API 24 or core library desugaring in the host app.
  public static void setEmitExperimentalTelemetry(
      OkHttpTelemetryBuilder builder, boolean emitExperimentalTelemetry) {
    if (setEmitExperimentalTelemetry != null) {
      setEmitExperimentalTelemetry.accept(builder, emitExperimentalTelemetry);
    }
  }

  public static void internalSetEmitExperimentalTelemetry(
      BiConsumer<OkHttpTelemetryBuilder, Boolean> setEmitExperimentalTelemetry) {
    Experimental.setEmitExperimentalTelemetry = setEmitExperimentalTelemetry;
  }

  private Experimental() {}
}
