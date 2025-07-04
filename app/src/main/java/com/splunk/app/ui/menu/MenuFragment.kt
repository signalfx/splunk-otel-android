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

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.splunk.app.R
import com.splunk.app.databinding.FragmentMenuBinding
import com.splunk.app.ui.BaseFragment
import com.splunk.app.ui.customtracking.CustomTrackingFragment
import com.splunk.app.ui.httpurlconnection.HttpURLConnectionFragment
import com.splunk.app.ui.okhttp.OkHttpFragment
import com.splunk.app.ui.webview.WebViewFragment
import com.splunk.app.util.ApiVariant
import com.splunk.app.util.FragmentAnimation
import com.splunk.app.util.SlowRenderingUtils
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.agent.api.extension.splunkRumId
import com.splunk.rum.integration.navigation.extension.navigation
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

class MenuFragment : BaseFragment<FragmentMenuBinding>() {

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentMenuBinding
        get() = FragmentMenuBinding::inflate

    override val titleRes: Int = R.string.menu_title

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.crashReportsIllegal.setOnClickListener(onClickListener)
        viewBinding.okhttpSampleCalls.setOnClickListener(onClickListener)
        viewBinding.httpurlconnection.setOnClickListener(onClickListener)
        viewBinding.webViewNextgen.setOnClickListener(onClickListener)
        viewBinding.webViewLegacy.setOnClickListener(onClickListener)
        viewBinding.menuCustomTrackingButton.setOnClickListener(onClickListener)

        viewBinding.setStringAttribute.setOnClickListener(onClickListener)
        viewBinding.setLongAttribute.setOnClickListener(onClickListener)
        viewBinding.setDoubleAttribute.setOnClickListener(onClickListener)
        viewBinding.setBooleanAttribute.setOnClickListener(onClickListener)
        viewBinding.setGenericAttribute.setOnClickListener(onClickListener)
        viewBinding.removeStringAttribute.setOnClickListener(onClickListener)
        viewBinding.removeGenericAttribute.setOnClickListener(onClickListener)
        viewBinding.getStringAttribute.setOnClickListener(onClickListener)
        viewBinding.getGenericAttribute.setOnClickListener(onClickListener)
        viewBinding.setAllGlobalAttributes.setOnClickListener(onClickListener)
        viewBinding.removeAllGlobalAttributes.setOnClickListener(onClickListener)
        viewBinding.getAllGlobalAttributes.setOnClickListener(onClickListener)
        viewBinding.legacySetGlobalAttribute.setOnClickListener(onClickListener)
        viewBinding.legacyUpdateGlobalAttributes.setOnClickListener(onClickListener)
        viewBinding.slowRender.setOnClickListener(onClickListener)
        viewBinding.frozenRender.setOnClickListener(onClickListener)
        viewBinding.crashReportsIllegal.splunkRumId = "illegalButton"

        SplunkRum.instance.navigation.track("Menu")
    }

    private val onClickListener = View.OnClickListener {
        when (it.id) {
            viewBinding.crashReportsIllegal.id ->
                throw IllegalArgumentException("Illegal Argument Exception Thrown!")
            viewBinding.okhttpSampleCalls.id ->
                navigateTo(OkHttpFragment(), FragmentAnimation.FADE)
            viewBinding.httpurlconnection.id ->
                navigateTo(HttpURLConnectionFragment(), FragmentAnimation.FADE)
            viewBinding.webViewNextgen.id -> {
                navigateTo(WebViewFragment.newInstance(ApiVariant.LATEST), FragmentAnimation.FADE)
            }
            viewBinding.webViewLegacy.id -> {
                navigateTo(WebViewFragment.newInstance(ApiVariant.LEGACY), FragmentAnimation.FADE)
            }
            viewBinding.menuCustomTrackingButton.id -> {
                navigateTo(CustomTrackingFragment(), FragmentAnimation.FADE)
            }
            viewBinding.setStringAttribute.id -> {
                SplunkRum.instance.globalAttributes["stringKey"] = "String Value"
                CommonUtils.showDoneToast(context, "Set String Global Attribute")
            }
            viewBinding.setLongAttribute.id -> {
                SplunkRum.instance.globalAttributes["longKey"] = 12345L
                CommonUtils.showDoneToast(context, "Set Long Global Attribute")
            }
            viewBinding.setDoubleAttribute.id -> {
                SplunkRum.instance.globalAttributes["doubleKey"] = 123.45
                CommonUtils.showDoneToast(context, "Set Double Global Attribute")
            }
            viewBinding.setBooleanAttribute.id -> {
                SplunkRum.instance.globalAttributes["booleanKey"] = true
                CommonUtils.showDoneToast(context, "Set Boolean Global Attribute")
            }
            viewBinding.setGenericAttribute.id -> {
                val key = AttributeKey.stringKey("genericKey")
                SplunkRum.instance.globalAttributes[key] = "Generic Value"
                CommonUtils.showDoneToast(context, "Set Generic Global Attribute")
            }
            viewBinding.removeStringAttribute.id -> {
                SplunkRum.instance.globalAttributes.remove("stringKey")
                CommonUtils.showDoneToast(context, "Remove String Global Attribute")
            }
            viewBinding.removeGenericAttribute.id -> {
                val key = AttributeKey.stringKey("genericKey")
                SplunkRum.instance.globalAttributes.remove(key)
                CommonUtils.showDoneToast(context, "Remove Generic Global Attribute")
            }
            viewBinding.getStringAttribute.id -> {
                val value: String? = SplunkRum.instance.globalAttributes["stringKey"]
                AlertDialog.Builder(context)
                    .setTitle("Key: stringKey")
                    .setMessage("Value: $value")
                    .setPositiveButton("OK", null)
                    .show()
            }
            viewBinding.getGenericAttribute.id -> {
                val key = AttributeKey.stringKey("genericKey")
                val value = SplunkRum.instance.globalAttributes[key]
                AlertDialog.Builder(context)
                    .setTitle("Key: genericKey")
                    .setMessage("Value: $value")
                    .setPositiveButton("OK", null)
                    .show()
            }
            viewBinding.setAllGlobalAttributes.id -> {
                @Suppress("ktlint:standard:argument-list-wrapping")
                val globalAttributes = Attributes.of(
                    AttributeKey.stringKey("setAllString"), "String Value",
                    AttributeKey.booleanKey("setAllBoolean"), true,
                    AttributeKey.doubleKey("setAllDouble"), 456.78,
                    AttributeKey.longKey("setAllLong"), 9876L
                )
                SplunkRum.instance.globalAttributes.setAll(globalAttributes)
                CommonUtils.showDoneToast(context, "Set All Global Attributes")
            }
            viewBinding.removeAllGlobalAttributes.id -> {
                SplunkRum.instance.globalAttributes.removeAll()
                CommonUtils.showDoneToast(context, "Remove All Global Attributes")
            }
            viewBinding.getAllGlobalAttributes.id -> {
                val allAttributes = SplunkRum.instance.globalAttributes
                val attributesText = StringBuilder()
                allAttributes.forEach { key, value ->
                    attributesText.append("${key.key}: $value\n")
                }

                AlertDialog.Builder(context)
                    .setTitle("All Global Attributes")
                    .setMessage(attributesText.toString())
                    .setPositiveButton("OK", null)
                    .show()
            }

            viewBinding.legacySetGlobalAttribute.id -> {
                val key = AttributeKey.stringKey("legacyKey")
                SplunkRum.instance.setGlobalAttribute(key, "LegacyVal")
                CommonUtils.showDoneToast(context, "Set Global Attribute using legacy API")
            }

            viewBinding.legacyUpdateGlobalAttributes.id -> {
                SplunkRum.instance.updateGlobalAttributes { builder ->
                    builder.put(AttributeKey.stringKey("legacyUpdate1"), "Value1")
                        .put(AttributeKey.longKey("legacyUpdate2"), 54321L)
                        .put(AttributeKey.booleanKey("legacyUpdate3"), false)
                }
                CommonUtils.showDoneToast(context, "Updated Global Attributes using legacy API")
            }

            viewBinding.slowRender.id -> {
                SlowRenderingUtils.simulateSlowRendering(
                    fragment = this,
                    testName = "slow render",
                    renderDelayMs = 30,
                    color = Color.BLUE
                )
            }

            viewBinding.frozenRender.id -> {
                SlowRenderingUtils.simulateSlowRendering(
                    fragment = this,
                    testName = "frozen render",
                    renderDelayMs = 800,
                    color = Color.RED,
                    refreshInterval = 1000
                )
            }
        }
    }
}
