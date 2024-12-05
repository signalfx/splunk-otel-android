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

package com.smartlook.app.ui.wireframe

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import com.cisco.android.rum.integration.recording.api.extension.isSensitive
import com.smartlook.app.R
import com.smartlook.app.databinding.FragmentWebViewBinding
import com.smartlook.app.ui.BaseFragment

@SuppressLint("SetJavaScriptEnabled")
class WebViewFragment : BaseFragment<FragmentWebViewBinding>() {

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentWebViewBinding
        get() = FragmentWebViewBinding::inflate

    override val titleRes: Int = R.string.web_view_title

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.webView1.isSensitive = true
        viewBinding.webView1.webViewClient = webViewClient
        viewBinding.webView1.settings.javaScriptEnabled = true
        viewBinding.webView1.settings.domStorageEnabled = true
        viewBinding.webView1.loadUrl("https://www.amazon.com/Oculus-Quest-Advanced-All-One-2/dp/B09P4F68WT/ref=mp_s_a_1_1_sspa?_encoding=UTF8&content-id=amzn1.sym.f2643a49-d441-49e2-8877-ba32df4e5cdc&keywords=oculus&pd_rd_r=c3ae64cb-e9b0-4894-a625-0efe81f01c0f&pd_rd_w=vzlqu&pd_rd_wg=DSoRq&pf_rd_p=f2643a49-d441-49e2-8877-ba32df4e5cdc&pf_rd_r=RBTGZK47X72SZZGVJE13&qid=1671092709&s=electronics&sr=1-1-spons&psc=1&spLa=ZW5jcnlwdGVkUXVhbGlmaWVyPUFUMjdYNDRNRFVEM0kmZW5jcnlwdGVkSWQ9QTA0OTEwMTQxMElWU01QR1I3NDdPJmVuY3J5cHRlZEFkSWQ9QTA5NzgyNjRYUkhSM1g2VUxDV08md2lkZ2V0TmFtZT1zcF9waG9uZV9zZWFyY2hfYXRmJmFjdGlvbj1jbGlja1JlZGlyZWN0JmRvTm90TG9nQ2xpY2s9dHJ1ZQ==")

        viewBinding.webView2.isSensitive = false
        viewBinding.webView2.webViewClient = webViewClient
        viewBinding.webView2.settings.javaScriptEnabled = true
        viewBinding.webView2.settings.domStorageEnabled = true
        viewBinding.webView2.loadUrl("https://www.quackit.com/html/html_editors/scratchpad/preview.cfm?example=/html/codes/html_form_code")
    }

    private val webViewClient = object : WebViewClient() {}
}
