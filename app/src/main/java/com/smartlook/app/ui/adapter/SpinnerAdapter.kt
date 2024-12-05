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

package com.smartlook.app.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.cisco.android.rum.integration.recording.api.extension.isSensitive
import com.smartlook.app.databinding.ItemExampleBinding
import com.smartlook.app.ui.wireframe.model.SimpleItem

class SpinnerAdapter(context: Context, items: List<SimpleItem>) : ArrayAdapter<SimpleItem>(context, 0, items) {

    private val inflater = LayoutInflater.from(context)

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ItemExampleBinding.inflate(inflater, parent, false)
        val item = getItem(position) ?: error("Null item")

        binding.title.text = item.title
        binding.description.isSensitive = true
        binding.description.text = item.description
        binding.root.setBackgroundColor(item.backgroundColor)

        return binding.root
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = getView(position, convertView, parent)
}
