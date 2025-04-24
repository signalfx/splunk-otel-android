package com.splunk.app.ui.webview;

import android.util.Log;
import android.webkit.WebView;

import com.splunk.rum.integration.agent.api.SplunkRum;

public class JavaTestUtil {
    public static void nextGen(WebView webview) {
        SplunkRum.getInstance().getWebViewNativeBridge().integrateWithBrowserRum(webview);
        Log.d("JavaTest", "Nextgen integrateWithBrowserRum API called");
    }

    public static void legacy(WebView webview) {
        SplunkRum.getInstance().integrateWithBrowserRum(webview);
        Log.d("JavaTest", "legacy integrateWithBrowserRum API called");
    }
}
