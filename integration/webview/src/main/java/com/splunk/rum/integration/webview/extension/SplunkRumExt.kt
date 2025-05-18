/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.integration.webview.extension

import android.webkit.WebView
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.webview.WebViewNativeBridge

/**
 * Extension property to access the [WebViewNativeBridge] instance via [SplunkRum].
 */
@Suppress("UnusedReceiverParameter")
val SplunkRum.webViewNativeBridge: WebViewNativeBridge
    get() = WebViewNativeBridge

/**
 * This method will enable Splunk Browser-based RUM to integrate with the current Android RUM
 * Session. It injects a javascript object named "SplunkRumNative" into your WebView which
 * exposes the Android Session ID to the browser-based RUM javascript implementation.
 *
 * @param webView The WebView to inject the javascript object into.
 */
@Deprecated(
    "Use SplunkRum.instance.webViewNativeBridge.integrateWithBrowserRum(webView)",
    ReplaceWith("SplunkRum.instance.webViewNativeBridge.integrateWithBrowserRum(webView)")
)
fun SplunkRum.integrateWithBrowserRum(webView: WebView) {
    webViewNativeBridge.integrateWithBrowserRum(webView)
}
