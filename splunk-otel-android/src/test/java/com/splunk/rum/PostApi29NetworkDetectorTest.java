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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.telephony.TelephonyManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.Q)
public class PostApi29NetworkDetectorTest {

    ConnectivityManager connectivityManager;
    TelephonyManager telephonyManager;
    Context context;
    Network network;
    NetworkCapabilities networkCapabilities;
    CarrierFinder carrierFinder;

    @Before
    public void setup() {
        connectivityManager = mock(ConnectivityManager.class);
        telephonyManager = mock(TelephonyManager.class);
        context = mock(Context.class);
        network = mock(Network.class);
        carrierFinder = mock(CarrierFinder.class);
        networkCapabilities = mock(NetworkCapabilities.class);

        when(connectivityManager.getActiveNetwork()).thenReturn(network);
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities);
    }

    @Test
    public void none() {
        when(connectivityManager.getNetworkCapabilities(network)).thenReturn(null);

        PostApi29NetworkDetector networkDetector =
                new PostApi29NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertEquals(new CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE), currentNetwork);
    }

    @Test
    public void wifi() {
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true);

        PostApi29NetworkDetector networkDetector =
                new PostApi29NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertEquals(new CurrentNetwork(NetworkState.TRANSPORT_WIFI), currentNetwork);
    }

    @Test
    public void cellular() {
        when(telephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_LTE);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);

        PostApi29NetworkDetector networkDetector =
                new PostApi29NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertEquals(new CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, "LTE"), currentNetwork);
    }

    @Test
    public void cellular_noTelephonyPermissions() {
        when(telephonyManager.getDataNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_LTE);
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);

        PostApi29NetworkDetector networkDetector =
                new PostApi29NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context) {
                    @Override
                    boolean hasPermission(String permission) {
                        return false;
                    }
                };
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertEquals(new CurrentNetwork(NetworkState.TRANSPORT_CELLULAR), currentNetwork);
    }

    @Test
    public void other() {
        PostApi29NetworkDetector networkDetector =
                new PostApi29NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertEquals(new CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN), currentNetwork);
    }

    @Test
    public void vpn() {
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)).thenReturn(true);

        PostApi29NetworkDetector networkDetector =
                new PostApi29NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertEquals(new CurrentNetwork(NetworkState.TRANSPORT_VPN), currentNetwork);
    }

    @Test
    public void carrierIsSet() {
        Carrier carrier = Carrier.builder().name("flib").build();
        when(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true);
        when(carrierFinder.get()).thenReturn(carrier);
        PostApi29NetworkDetector networkDetector =
                new PostApi29NetworkDetector(
                        connectivityManager, telephonyManager, carrierFinder, context);
        CurrentNetwork currentNetwork = networkDetector.detectCurrentNetwork();
        assertThat(currentNetwork.getCarrierName()).isEqualTo("flib");
    }
}
