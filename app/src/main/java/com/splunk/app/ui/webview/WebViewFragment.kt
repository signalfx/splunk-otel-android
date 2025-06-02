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

package com.splunk.app.ui.webview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.splunk.app.R
import com.splunk.app.databinding.FragmentWebViewBinding
import com.splunk.app.ui.BaseFragment
import com.splunk.app.util.ApiVariant
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.navigation.extension.navigation
import com.splunk.rum.integration.webview.extension.integrateWithBrowserRum
import com.splunk.rum.integration.webview.extension.webViewNativeBridge

class WebViewFragment : BaseFragment<FragmentWebViewBinding>() {

    override val titleRes: Int = R.string.webview_title
    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentWebViewBinding
        get() = FragmentWebViewBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.webView.settings.javaScriptEnabled = true

        val apiVariant = arguments?.getString("API_VARIANT")?.let { ApiVariant.valueOf(it) }

        when (apiVariant) {
            ApiVariant.NEXTGEN -> {
                SplunkRum.instance.webViewNativeBridge.integrateWithBrowserRum(viewBinding.webView)
                Log.d(TAG, "Nextgen integrateWithBrowserRum API called")
            }

            ApiVariant.LEGACY -> {
                SplunkRum.instance.integrateWithBrowserRum(viewBinding.webView)
                Log.d(TAG, "Legacy integrateWithBrowserRum API called")
            }

            null -> Log.e(TAG, "WebView not integrated with Browser RUM due to missing API_VARIANT argument.")
        }

        viewBinding.webView.loadUrl("file:///android_asset/webview_content.html")

        SplunkRum.instance.navigation.track("WebView")
    }

    override fun onDestroyView() {
        viewBinding.webView.destroy()
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "WebView"
    }
}
