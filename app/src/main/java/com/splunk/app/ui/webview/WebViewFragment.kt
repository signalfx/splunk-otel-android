package com.splunk.app.ui.webview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.splunk.app.R
import com.splunk.app.databinding.FragmentWebViewBinding
import com.splunk.app.ui.BaseFragment
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

        val implementationType = arguments?.getString("IMPLEMENTATION_TYPE")

        when (implementationType) {
            "nextgen" -> {
                SplunkRum.instance.webViewNativeBridge.integrateWithBrowserRum(viewBinding.webView)
                Log.d(TAG, "Nextgen integrateWithBrowserRum API called")
            }
            "legacy" -> {
                SplunkRum.instance.integrateWithBrowserRum(viewBinding.webView)
                Log.d(TAG, "Legacy integrateWithBrowserRum API called")
            }
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