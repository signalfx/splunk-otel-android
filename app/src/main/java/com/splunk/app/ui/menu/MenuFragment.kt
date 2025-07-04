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

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.splunk.app.R
import com.splunk.app.databinding.FragmentMenuBinding
import com.splunk.app.ui.BaseFragment
import com.splunk.app.ui.crashreports.CrashReportsFragment
import com.splunk.app.ui.customtracking.CustomTrackingFragment
import com.splunk.app.ui.globalattributes.GlobalAttributesFragment
import com.splunk.app.ui.httpurlconnection.HttpURLConnectionFragment
import com.splunk.app.ui.okhttp.OkHttpFragment
import com.splunk.app.ui.webview.WebViewFragment
import com.splunk.app.util.ApiVariant
import com.splunk.app.util.FragmentAnimation
import com.splunk.app.util.SlowRenderingUtils

class MenuFragment : BaseFragment<FragmentMenuBinding>() {

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentMenuBinding
        get() = FragmentMenuBinding::inflate

    override val titleRes: Int = R.string.menu_title

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            crashReports.setOnClickListener { openCrashReports() }
            okhttpSampleCalls.setOnClickListener { openOkHttpSamples() }
            httpUrlConnection.setOnClickListener { openHttpUrlConnection() }
            webViewLatest.setOnClickListener { openWebViewLatest() }
            webViewLegacy.setOnClickListener { openWebViewLegacy() }
            menuCustomTracking.setOnClickListener { openCustomTracking() }
            globalAttributes.setOnClickListener { openGlobalAttributes() }
            slowRender.setOnClickListener { simulateSlowRender() }
            frozenRender.setOnClickListener { simulateFrozenRender() }
        }
    }

    private fun openCrashReports() {
        navigateTo(CrashReportsFragment(), FragmentAnimation.FADE)
    }

    private fun openOkHttpSamples() {
        navigateTo(OkHttpFragment(), FragmentAnimation.FADE)
    }

    private fun openHttpUrlConnection() {
        navigateTo(HttpURLConnectionFragment(), FragmentAnimation.FADE)
    }

    private fun openWebViewLatest() {
        navigateTo(WebViewFragment.newInstance(ApiVariant.LATEST), FragmentAnimation.FADE)
    }

    private fun openWebViewLegacy() {
        navigateTo(WebViewFragment.newInstance(ApiVariant.LEGACY), FragmentAnimation.FADE)
    }

    private fun openCustomTracking() {
        navigateTo(CustomTrackingFragment(), FragmentAnimation.FADE)
    }

    private fun openGlobalAttributes() {
        navigateTo(GlobalAttributesFragment(), FragmentAnimation.FADE)
    }

    private fun simulateSlowRender() {
        SlowRenderingUtils.simulateSlowRendering(
            fragment = this,
            testName = "slow render",
            renderDelayMs = 30,
            color = Color.BLUE
        )
    }

    private fun simulateFrozenRender() {
        SlowRenderingUtils.simulateSlowRendering(
            fragment = this,
            testName = "frozen render",
            renderDelayMs = 800,
            color = Color.RED,
            refreshInterval = 1000
        )
    }
}
