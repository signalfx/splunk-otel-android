package com.splunk.rum;

public class Config {

    private final String beaconUrl;
    private final String rumAuthToken;

    private Config(Builder builder) {
        this.beaconUrl = builder.beaconUrl;
        this.rumAuthToken = builder.rumAuthToken;
    }

    public String getBeaconUrl() {
        return beaconUrl;
    }

    public String getRumAuthToken() {
        return rumAuthToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String beaconUrl;
        private String rumAuthToken;

        public Config build() {
            if (rumAuthToken == null || beaconUrl == null) {
                throw new IllegalStateException("You must provide both a rumAuthToken and a beaconUrl to create a valid Config instance.");
            }
            return new Config(this);
        }

        public Builder beaconUrl(String beaconUrl) {
            this.beaconUrl = beaconUrl;
            return this;
        }

        public Builder rumAuthToken(String rumAuthToken) {
            this.rumAuthToken = rumAuthToken;
            return this;
        }
    }
}
