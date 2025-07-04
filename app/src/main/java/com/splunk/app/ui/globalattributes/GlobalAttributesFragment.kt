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

package com.splunk.app.ui.globalattributes

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.splunk.app.R
import com.splunk.app.databinding.FragmentGlobalAttributesBinding
import com.splunk.app.extension.showDoneToast
import com.splunk.app.ui.BaseFragment
import com.splunk.app.util.ApiVariant
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

class GlobalAttributesFragment : BaseFragment<FragmentGlobalAttributesBinding>() {

    override val titleRes: Int = R.string.global_attributes_title

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentGlobalAttributesBinding
        get() = FragmentGlobalAttributesBinding::inflate

    private val splunkRum = SplunkRum.instance
    private val globalAttributes = SplunkRum.instance.globalAttributes

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            setStringAttribute.setOnClickListener { setStringAttribute() }
            setLongAttribute.setOnClickListener { setLongAttribute() }
            setDoubleAttribute.setOnClickListener { setDoubleAttribute() }
            setBooleanAttribute.setOnClickListener { setBooleanAttribute() }
            setGenericAttribute.setOnClickListener { setGenericAttribute() }
            removeStringAttribute.setOnClickListener { removeStringAttribute() }
            removeGenericAttribute.setOnClickListener { removeGenericAttribute() }
            getStringAttribute.setOnClickListener { getStringAttribute() }
            getGenericAttribute.setOnClickListener { getGenericAttribute() }
            setAllGlobalAttributes.setOnClickListener { setAllGlobalAttributes() }
            removeAllGlobalAttributes.setOnClickListener { removeAllGlobalAttributes() }
            getAllGlobalAttributes.setOnClickListener { getAllGlobalAttributes() }
            legacySetGlobalAttribute.setOnClickListener { legacySetGlobalAttribute() }
            legacyUpdateGlobalAttributes.setOnClickListener { legacyUpdateGlobalAttributes() }
        }
    }

    private fun setStringAttribute() {
        globalAttributes["stringKey"] = "String Value"
        context?.showDoneToast(R.string.set_string_global_attribute)
    }

    private fun setLongAttribute() {
        globalAttributes["longKey"] = 12345L
        context?.showDoneToast(R.string.set_long_global_attribute)
    }

    private fun setDoubleAttribute() {
        globalAttributes["doubleKey"] = 123.45
        context?.showDoneToast(R.string.set_double_global_attribute)
    }

    private fun setBooleanAttribute() {
        globalAttributes["booleanKey"] = true
        context?.showDoneToast(R.string.set_boolean_global_attribute)
    }

    private fun setGenericAttribute() {
        val key = AttributeKey.stringKey("genericKey")
        globalAttributes[key] = "Generic Value"
        context?.showDoneToast(R.string.set_generic_global_attribute)
    }

    private fun removeStringAttribute() {
        globalAttributes.remove("stringKey")
        context?.showDoneToast(R.string.remove_string_global_attribute)
    }

    private fun removeGenericAttribute() {
        val key = AttributeKey.stringKey("genericKey")
        globalAttributes.remove(key)
        context?.showDoneToast(R.string.remove_generic_global_attribute)
    }

    private fun getStringAttribute() {
        val value: String? = globalAttributes["stringKey"]
        AlertDialog.Builder(context)
            .setTitle("Key: stringKey")
            .setMessage("Value: $value")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun getGenericAttribute() {
        val key = AttributeKey.stringKey("genericKey")
        val value = globalAttributes[key]
        AlertDialog.Builder(context)
            .setTitle("Key: genericKey")
            .setMessage("Value: $value")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setAllGlobalAttributes() {
        val attributes = Attributes.of(
            AttributeKey.stringKey("setAllString"), "String Value",
            AttributeKey.booleanKey("setAllBoolean"), true,
            AttributeKey.doubleKey("setAllDouble"), 456.78,
            AttributeKey.longKey("setAllLong"), 9876L
        )
        globalAttributes.setAll(attributes)
        context?.showDoneToast(R.string.set_all_global_attributes)
    }

    private fun removeAllGlobalAttributes() {
        globalAttributes.removeAll()
        context?.showDoneToast(R.string.remove_all_global_attributes)
    }

    private fun getAllGlobalAttributes() {
        val attributesText = StringBuilder()
        globalAttributes.forEach { key, value ->
            attributesText.append("${key.key}: $value\n")
        }

        AlertDialog.Builder(context)
            .setTitle("All Global Attributes")
            .setMessage(attributesText.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun legacySetGlobalAttribute() {
        val key = AttributeKey.stringKey("legacyKey")
        splunkRum.setGlobalAttribute(key, "LegacyVal")
        context?.showDoneToast(R.string.set_string_global_attribute, ApiVariant.LEGACY)
    }

    private fun legacyUpdateGlobalAttributes() {
        splunkRum.updateGlobalAttributes { builder ->
            builder.put(AttributeKey.stringKey("legacyUpdate1"), "Value1")
                .put(AttributeKey.longKey("legacyUpdate2"), 54321L)
                .put(AttributeKey.booleanKey("legacyUpdate3"), false)
        }
        context?.showDoneToast(R.string.update_global_attributes, ApiVariant.LEGACY)
    }
}
