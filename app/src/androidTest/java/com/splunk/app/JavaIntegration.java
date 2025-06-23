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

package com.splunk.app;

import android.app.Application;
import android.view.View;

import com.splunk.rum.integration.agent.api.AgentConfiguration;
import com.splunk.rum.integration.agent.api.EndpointConfiguration;
import com.splunk.rum.integration.agent.api.session.ISession;
import com.splunk.rum.integration.agent.api.IState;
import com.splunk.rum.integration.agent.api.SplunkRum;
import com.splunk.rum.integration.agent.api.session.SessionConfiguration;
import com.splunk.rum.integration.agent.api.user.UserConfiguration;
import com.splunk.rum.integration.agent.api.user.UserState;
import com.splunk.rum.integration.agent.api.user.UserTrackingMode;
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes;
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration;
import com.splunk.rum.integration.anr.AnrModuleConfiguration;
import com.splunk.rum.integration.crash.CrashModuleConfiguration;
import com.splunk.rum.integration.customtracking.CustomTracking;
import com.splunk.rum.integration.httpurlconnection.auto.HttpURLModuleConfiguration;
import com.splunk.rum.integration.interactions.InteractionsModuleConfiguration;
import com.splunk.rum.integration.lifecycle.LifecycleModuleConfiguration;
import com.splunk.rum.integration.navigation.Navigation;
import com.splunk.rum.integration.navigation.NavigationModuleConfiguration;
import com.splunk.rum.integration.networkmonitor.NetworkMonitorModuleConfiguration;
import com.splunk.rum.integration.okhttp3.auto.OkHttp3AutoModuleConfiguration;
import com.splunk.rum.integration.okhttp3.manual.OkHttp3ManualModuleConfiguration;
import com.splunk.rum.integration.okhttp3.manual.extension.SplunkRumExtKt;
import com.splunk.rum.integration.sessionreplay.SessionReplayModuleConfiguration;
import com.splunk.rum.integration.slowrendering.SlowRenderingModuleConfiguration;
import com.splunk.rum.integration.startup.StartupModuleConfiguration;

import java.time.Duration;
import java.util.Arrays;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

public class JavaIntegration extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        SplunkRum agent = install();

        agent.getRumSessionId();

        SplunkRum.noop();

        IState state = agent.getState();
        state.getAppName();
        state.getAppVersion();
        state.getInstrumentedProcessName();
        state.getEndpointConfiguration();
        state.isDebugLoggingEnabled();
        state.getStatus();

        UserState userState = agent.getUser().getState();
        userState.getTrackingMode();

        ISession session = agent.getSession();
        session.getState().getSessionId();
        session.getState().getSamplingRate();

        agent.getOpenTelemetry();

        CustomTracking tracking = CustomTracking.getInstance();
        tracking.trackCustomEvent("event", Attributes.of(AttributeKey.stringKey("key"), "value"));
        tracking.trackException(new IllegalArgumentException());

        Navigation navigation = Navigation.getInstance();
        navigation.track("sample_screen_name");
    }

    private SplunkRum install() {
        MutableAttributes globalAttributes = new MutableAttributes();
        globalAttributes.set("string", "string");
        globalAttributes.set("int", 1);

        AgentConfiguration configuration = new AgentConfiguration(
                new EndpointConfiguration("sample_realm", "sample_token"),
                "Test",
                "dev",
                "1.0",
                true,
                globalAttributes,
                (spanData) -> spanData,
                new UserConfiguration(UserTrackingMode.ANONYMOUS_TRACKING),
                new SessionConfiguration(0.8),
                "app",
                false
        );

        ModuleConfiguration[] moduleConfigurations = new ModuleConfiguration[]{
                new AnrModuleConfiguration(),
                new CrashModuleConfiguration(),
                new HttpURLModuleConfiguration(
                        true,
                        Arrays.asList("Host", "Accept"),
                        Arrays.asList("Date", "Content-Type", "Content-Length")
                ),
                new InteractionsModuleConfiguration(),
                new LifecycleModuleConfiguration(),
                new NavigationModuleConfiguration(true, true),
                new NetworkMonitorModuleConfiguration(),
                new OkHttp3AutoModuleConfiguration( true,
                        Arrays.asList("User-Agent", "Accept"),
                        Arrays.asList("Date", "Content-Type", "Content-Length")
                ),
                new OkHttp3ManualModuleConfiguration(
                        Arrays.asList("Content-Type", "Accept"),
                        Arrays.asList("Server", "Content-Type", "Content-Length")
                ),
                new SessionReplayModuleConfiguration(),
                new SlowRenderingModuleConfiguration(true),
                new StartupModuleConfiguration()
        };

        return SplunkRum.install(this, configuration, moduleConfigurations);
    }

    private SplunkRum legacyBuilder() {
        return SplunkRum.builder()
                .setRealm("lab0")
                .setRumAccessToken("1CucSUVwF5f2hNyuHwKNfw")
                .setApplicationName("Android demo app")
                .setDeploymentEnvironment("test")
                .setGlobalAttributes(Attributes.of(AttributeKey.stringKey("legacyGlobalAttributesKey"), "legacyGlobalAttributesVal"))
                .enableDebug()
                .disableAnrDetection()
                .disableCrashReporting()
                .disableSlowRenderingDetection()
                .setSlowRenderingDetectionPollInterval(Duration.ofMillis(500))
                .disableNetworkMonitor()
                .build(this);
    }
}
