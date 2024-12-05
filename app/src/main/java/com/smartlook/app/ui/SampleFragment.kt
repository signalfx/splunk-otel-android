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

package com.smartlook.app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.smartlook.app.databinding.ItemExampleBinding

class SampleFragment : Fragment() {

    private lateinit var viewBinding: ItemExampleBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = ItemExampleBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.title.text = arguments?.getString("title")
        viewBinding.description.text = arguments?.getString("description")
        viewBinding.root.setBackgroundColor(arguments?.getInt("color") ?: Color.TRANSPARENT)
    }

    companion object {

        fun create(title: String, description: String, color: Int): SampleFragment {
            val fragment = SampleFragment()
            fragment.arguments = bundleOf(
                "title" to title,
                "description" to description,
                "color" to color
            )

            return fragment
        }
    }
}
