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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import com.smartlook.app.R
import com.smartlook.app.databinding.FragmentWireframeListBinding
import com.smartlook.app.ui.BaseFragment

class ListFragment : BaseFragment<FragmentWireframeListBinding>() {

    override val titleRes: Int = R.string.list_title
    override val subtitleRes: Int? = null

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentWireframeListBinding
        get() = FragmentWireframeListBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = (0..100).map { "Title $it" }
        viewBinding.list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
        viewBinding.list.onItemClickListener = onItemClickListener
    }

    private val onItemClickListener = OnItemClickListener { _, _, position, _ ->
        Toast.makeText(requireContext(), "$position clicked", Toast.LENGTH_SHORT).show()
    }
}
