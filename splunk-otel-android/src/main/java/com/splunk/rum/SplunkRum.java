package com.splunk.rum;

import android.util.Log;

public class SplunkRum {

    private static final String LOG_TAG = "SplunkRum";

    private static SplunkRum INSTANCE;

    private final Config config;

    private SplunkRum(Config config) {
        this.config = config;
    }

    public static Config.Builder newConfigBuilder() {
        return Config.builder();
    }

    public static SplunkRum initialize(Config config) {
        if (INSTANCE != null) {
            Log.w(LOG_TAG, "Singleton SplunkRum instance has already been initialized.");
            return INSTANCE;
        }
        INSTANCE = new SplunkRum(config);
        return INSTANCE;
    }

    public static SplunkRum getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SplunkRum has not been initialized.");
        }
        return INSTANCE;
    }

    //for testing only
    static void resetSingletonForTest() {
        INSTANCE = null;
    }
}
