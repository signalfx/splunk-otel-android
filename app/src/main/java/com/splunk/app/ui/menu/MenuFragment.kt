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

package com.splunk.app.ui.menu

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.splunk.app.R
import com.splunk.app.databinding.FragmentMenuBinding
import com.splunk.app.extension.showToast
import com.splunk.app.restart.TerminationWatcherService
import com.splunk.app.ui.BaseFragment
import com.splunk.app.ui.crashreports.CrashReportsFragment
import com.splunk.app.ui.customtracking.CustomTrackingFragment
import com.splunk.app.ui.endpointconfiguration.EndpointConfigurationFragment
import com.splunk.app.ui.globalattributes.GlobalAttributesFragment
import com.splunk.app.ui.httpurlconnection.HttpURLConnectionFragment
import com.splunk.app.ui.okhttp.OkHttpFragment
import com.splunk.app.ui.slowrendering.SlowRenderingFragment
import com.splunk.app.ui.webview.WebViewFragment
import com.splunk.app.util.ApiVariant
import com.splunk.app.util.FragmentAnimation

class MenuFragment : BaseFragment<FragmentMenuBinding>() {

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentMenuBinding
        get() = FragmentMenuBinding::inflate

    override val titleRes: Int = R.string.menu_title

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            startupRestart.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

            crashReports.setOnClickListener {
                navigateTo(CrashReportsFragment(), FragmentAnimation.FADE)
            }
            okhttpSampleCalls.setOnClickListener {
                navigateTo(OkHttpFragment(), FragmentAnimation.FADE)
            }
            httpUrlConnection.setOnClickListener {
                navigateTo(HttpURLConnectionFragment(), FragmentAnimation.FADE)
            }
            webViewLatest.setOnClickListener {
                navigateTo(WebViewFragment.newInstance(ApiVariant.LATEST), FragmentAnimation.FADE)
            }
            webViewLegacy.setOnClickListener {
                navigateTo(WebViewFragment.newInstance(ApiVariant.LEGACY), FragmentAnimation.FADE)
            }
            menuCustomTracking.setOnClickListener {
                navigateTo(CustomTrackingFragment(), FragmentAnimation.FADE)
            }
            globalAttributes.setOnClickListener {
                navigateTo(GlobalAttributesFragment(), FragmentAnimation.FADE)
            }
            slowRendering.setOnClickListener {
                navigateTo(SlowRenderingFragment(), FragmentAnimation.FADE)
            }
            endpointConfiguration.setOnClickListener {
                navigateTo(EndpointConfigurationFragment(), FragmentAnimation.FADE)
            }
            startupRestart.setOnClickListener {
                TerminationWatcherService.restartOnClose(requireContext())
                showToast(R.string.startup_restart_scheduled)
            }
        }
    }
}
