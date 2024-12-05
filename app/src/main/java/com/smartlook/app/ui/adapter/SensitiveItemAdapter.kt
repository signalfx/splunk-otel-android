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
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.cisco.android.rum.integration.recording.api.extension.isSensitive
import com.smartlook.app.databinding.ItemSensitiveBinding

class SensitiveItemAdapter(
    context: Context,
    private val count: Int
) : RecyclerView.Adapter<SensitiveItemAdapter.ItemViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    override fun getItemCount(): Int {
        return count
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemSensitiveBinding.inflate(inflater, parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.binding.a.isSensitive = true
        holder.binding.b.isSensitive = true
        holder.binding.c.isSensitive = true

        val context = holder.itemView.context
        holder.binding.a.setOnClickListener { Toast.makeText(context, "A", Toast.LENGTH_SHORT).show() }
        holder.binding.b.setOnClickListener { Toast.makeText(context, "B", Toast.LENGTH_SHORT).show() }
        holder.binding.c.setOnClickListener { Toast.makeText(context, "C", Toast.LENGTH_SHORT).show() }
    }

    class ItemViewHolder(val binding: ItemSensitiveBinding) : RecyclerView.ViewHolder(binding.root)
}
