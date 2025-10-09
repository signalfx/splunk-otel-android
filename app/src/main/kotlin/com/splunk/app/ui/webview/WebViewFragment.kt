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

import android.annotation.SuppressLint
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
import com.splunk.rum.integration.webview.extension.webViewNativeBridge

/**
 * A fragment that demonstrates how to instrument a WebView with Splunk RUM's Browser RUM integration.
 *
 * This fragment loads a local HTML file from `assets/` and integrates with Splunk RUM using one of two
 * supported integration APIs: `LEGACY` or `LATEST`, based on the passed-in [ApiVariant] argument.
 */
class WebViewFragment : BaseFragment<FragmentWebViewBinding>() {

    override val titleRes: Int = R.string.web_view_title

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentWebViewBinding
        get() = FragmentWebViewBinding::inflate

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val apiVariant = runCatching {
            arguments?.getString(ARG_API_VARIANT)?.let { ApiVariant.valueOf(it) }
        }.getOrNull()

        viewBinding.webView.settings.javaScriptEnabled = true

        when (apiVariant) {
            ApiVariant.LATEST -> {
                SplunkRum.instance.webViewNativeBridge.integrateWithBrowserRum(viewBinding.webView)
                Log.d(TAG, "Latest integrateWithBrowserRum API called")
            }
            ApiVariant.LEGACY -> {
                SplunkRum.instance.integrateWithBrowserRum(viewBinding.webView)
                Log.d(TAG, "Legacy integrateWithBrowserRum API called")
            }
            null -> Log.e(TAG, "WebView not integrated with Browser RUM due to missing API_VARIANT argument.")
        }

        viewBinding.webView.loadUrl("file:///android_asset/webview_content.html")
    }

    override fun onDestroyView() {
        viewBinding.webView.destroy()
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "WebView"
        private const val ARG_API_VARIANT = "API_VARIANT"

        fun newInstance(variant: ApiVariant): WebViewFragment = WebViewFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_API_VARIANT, variant.name)
            }
        }
    }
}
