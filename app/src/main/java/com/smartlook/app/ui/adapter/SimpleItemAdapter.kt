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

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartlook.app.databinding.ItemExampleBinding
import com.smartlook.app.ui.wireframe.model.SimpleItem

class SimpleItemAdapter(
    context: Context,
    private val list: List<SimpleItem>
) : RecyclerView.Adapter<SimpleItemAdapter.SimpleItemViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleItemViewHolder {
        val binding = ItemExampleBinding.inflate(inflater, parent, false)
        return SimpleItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SimpleItemViewHolder, position: Int) {
        holder.bind(list[position])
    }

    class SimpleItemViewHolder(
        private val binding: ItemExampleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SimpleItem) {
            binding.title.text = item.title
            binding.description.text = item.description
            binding.root.setBackgroundColor(item.backgroundColor)
        }
    }
}
