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

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_ICC;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_MCC;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_MNC;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_TYPE;

import android.os.Build;
import androidx.annotation.Nullable;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.Objects;

final class CurrentNetwork {
    @Nullable private final Carrier carrier;
    private final NetworkState state;
    @Nullable private final String subType;

    private CurrentNetwork(Builder builder) {
        this.carrier = builder.carrier;
        this.state = builder.state;
        this.subType = builder.subType;
    }

    boolean isOnline() {
        return getState() != NetworkState.NO_NETWORK_AVAILABLE;
    }

    NetworkState getState() {
        return state;
    }

    Attributes getNetworkAttributes() {
        AttributesBuilder builder =
                Attributes.builder().put(NET_HOST_CONNECTION_TYPE, state.getHumanName());

        setIfNotNull(builder, NET_HOST_CONNECTION_SUBTYPE, subType);
        if (haveCarrier()) {
            setIfNotNull(builder, NET_HOST_CARRIER_NAME, carrier.getName());
            setIfNotNull(builder, NET_HOST_CARRIER_MCC, carrier.getMobileCountryCode());
            setIfNotNull(builder, NET_HOST_CARRIER_MNC, carrier.getMobileNetworkCode());
            setIfNotNull(builder, NET_HOST_CARRIER_ICC, carrier.getIsoCountryCode());
        }

        return builder.build();
    }

    private boolean haveCarrier() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) && (carrier != null);
    }

    private static void setIfNotNull(
            AttributesBuilder builder, AttributeKey<String> key, @Nullable String value) {
        if (value != null) {
            builder.put(key, value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrentNetwork that = (CurrentNetwork) o;
        return Objects.equals(carrier, that.carrier)
                && state == that.state
                && Objects.equals(subType, that.subType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrier, state, subType);
    }

    @Override
    public String toString() {
        return "CurrentNetwork{"
                + "carrier="
                + carrier
                + ", state="
                + state
                + ", subType='"
                + subType
                + '\''
                + '}';
    }

    static Builder builder(NetworkState state) {
        return new Builder(state);
    }

    static class Builder {
        @Nullable private Carrier carrier;
        private final NetworkState state;
        @Nullable private String subType;

        public Builder(NetworkState state) {
            this.state = state;
        }

        CurrentNetwork build() {
            return new CurrentNetwork(this);
        }

        public Builder carrier(@Nullable Carrier carrier) {
            this.carrier = carrier;
            return this;
        }

        public Builder subType(@Nullable String subType) {
            this.subType = subType;
            return this;
        }
    }
}
