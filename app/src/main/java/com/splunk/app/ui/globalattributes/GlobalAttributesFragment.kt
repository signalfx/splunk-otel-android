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
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

/**
 * Fragment for demonstrating the usage of Global Attributes in Splunk RUM.
 *
 * This screen allows users to:
 * - Set and remove various global attribute types (String, Long, Double, Boolean, generic types)
 * - Retrieve individual or all global attributes
 * - Use both latest and legacy APIs to modify global attributes
 */
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

    /** Sets a global attribute with a String value. */
    private fun setStringAttribute() {
        globalAttributes["stringKey"] = "String Value"
        showDoneToast(R.string.set_string_global_attribute)
    }

    /** Sets a global attribute with a Long value. */
    private fun setLongAttribute() {
        globalAttributes["longKey"] = 12345L
        showDoneToast(R.string.set_long_global_attribute)
    }

    /** Sets a global attribute with a Double value. */
    private fun setDoubleAttribute() {
        globalAttributes["doubleKey"] = 123.45
        showDoneToast(R.string.set_double_global_attribute)
    }

    /** Sets a global attribute with a Boolean value. */
    private fun setBooleanAttribute() {
        globalAttributes["booleanKey"] = true
        showDoneToast(R.string.set_boolean_global_attribute)
    }

    /** Sets a global attribute using a generic [AttributeKey]. */
    private fun setGenericAttribute() {
        val key = AttributeKey.stringKey("genericKey")
        globalAttributes[key] = "Generic Value"
        showDoneToast(R.string.set_generic_global_attribute)
    }

    /** Removes a global attribute by its string key. */
    private fun removeStringAttribute() {
        globalAttributes.remove("stringKey")
        showDoneToast(R.string.remove_string_global_attribute)
    }

    /** Removes a global attribute using a generic [AttributeKey]. */
    private fun removeGenericAttribute() {
        val key = AttributeKey.stringKey("genericKey")
        globalAttributes.remove(key)
        showDoneToast(R.string.remove_generic_global_attribute)
    }

    /** Retrieves and displays a global String attribute in an alert dialog. */
    private fun getStringAttribute() {
        val value: String? = globalAttributes["stringKey"]
        AlertDialog.Builder(context)
            .setTitle("Key: stringKey")
            .setMessage("Value: $value")
            .setPositiveButton("OK", null)
            .show()
    }

    /** Retrieves and displays a global attribute set via a generic [AttributeKey]. */
    private fun getGenericAttribute() {
        val key = AttributeKey.stringKey("genericKey")
        val value = globalAttributes[key]
        AlertDialog.Builder(context)
            .setTitle("Key: genericKey")
            .setMessage("Value: $value")
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * Sets multiple global attributes at once using the `setAll()` method.
     */
    private fun setAllGlobalAttributes() {
        val attributes = Attributes.of(
            AttributeKey.stringKey("setAllString"),
            "String Value",
            AttributeKey.booleanKey("setAllBoolean"),
            true,
            AttributeKey.doubleKey("setAllDouble"),
            456.78,
            AttributeKey.longKey("setAllLong"),
            9876L
        )
        globalAttributes.setAll(attributes)
        showDoneToast(R.string.set_all_global_attributes)
    }

    /** Removes all currently set global attributes. */
    private fun removeAllGlobalAttributes() {
        globalAttributes.removeAll()
        showDoneToast(R.string.remove_all_global_attributes)
    }

    /**
     * Retrieves and displays all global attributes in a dialog.
     */
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

    /**
     * Uses the legacy Splunk RUM API to set a single global attribute.
     */
    private fun legacySetGlobalAttribute() {
        val key = AttributeKey.stringKey("legacyKey")
        splunkRum.setGlobalAttribute(key, "LegacyVal")
        showDoneToast(R.string.set_string_global_attribute, ApiVariant.LEGACY)
    }

    /**
     * Uses the legacy Splunk RUM API to update multiple global attributes at once.
     */
    private fun legacyUpdateGlobalAttributes() {
        splunkRum.updateGlobalAttributes { builder ->
            builder.put(AttributeKey.stringKey("legacyUpdate1"), "Value1")
                .put(AttributeKey.longKey("legacyUpdate2"), 54321L)
                .put(AttributeKey.booleanKey("legacyUpdate3"), false)
        }
        showDoneToast(R.string.update_global_attributes, ApiVariant.LEGACY)
    }
}
