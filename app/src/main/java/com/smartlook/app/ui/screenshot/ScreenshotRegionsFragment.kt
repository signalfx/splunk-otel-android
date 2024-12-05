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

package com.smartlook.app.ui.screenshot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cisco.android.rum.integration.recording.api.extension.isSensitive
import com.smartlook.app.R
import com.smartlook.app.databinding.FragmentScreenshotRegionsBinding
import com.smartlook.app.ui.BaseFragment

class ScreenshotRegionsFragment : BaseFragment<FragmentScreenshotRegionsBinding>() {

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentScreenshotRegionsBinding
        get() = FragmentScreenshotRegionsBinding::inflate

    override val titleRes: Int = R.string.screenshot_fragment_regions_title

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.region1a.isSensitive = true
        viewBinding.region1b.isSensitive = true
        viewBinding.region1c.isSensitive = true
        viewBinding.region1d.isSensitive = true
        viewBinding.region1e.isSensitive = true
        viewBinding.region1f.isSensitive = true
        viewBinding.region2.isSensitive = true
        viewBinding.region3.isSensitive = true
        viewBinding.region4.isSensitive = true
        viewBinding.region5.isSensitive = true
        viewBinding.region6.isSensitive = true
        viewBinding.region7a.isSensitive = true
        viewBinding.region7b.isSensitive = true
        viewBinding.region8a.isSensitive = true
        viewBinding.region8b.isSensitive = true
        viewBinding.region9a.isSensitive = true
        viewBinding.region9b.isSensitive = true
        viewBinding.region10a.isSensitive = true
        viewBinding.region10b.isSensitive = true
        viewBinding.region11a.isSensitive = true
        viewBinding.region11b.isSensitive = true
        viewBinding.region11c.isSensitive = true
        viewBinding.region11d.isSensitive = true
        viewBinding.region11e.isSensitive = true
        viewBinding.region11f.isSensitive = true
        viewBinding.region12a.isSensitive = true
        viewBinding.region12b.isSensitive = true
        viewBinding.region12c.isSensitive = true
        viewBinding.region12d.isSensitive = true
        viewBinding.region12e.isSensitive = true
        viewBinding.region12f.isSensitive = true
        viewBinding.region13a.isSensitive = true
        viewBinding.region13b.isSensitive = true
        viewBinding.region13c.isSensitive = true
        viewBinding.region13d.isSensitive = true
        viewBinding.region13e.isSensitive = true
        viewBinding.region13f.isSensitive = true
        viewBinding.region14a.isSensitive = true
        viewBinding.region14b.isSensitive = true
        viewBinding.region14c.isSensitive = true
        viewBinding.region14d.isSensitive = true
        viewBinding.region14e.isSensitive = true
        viewBinding.region14f.isSensitive = true
        viewBinding.region15a.isSensitive = true
        viewBinding.region15b.isSensitive = true
        viewBinding.region15c.isSensitive = true
        viewBinding.region15d.isSensitive = true
        viewBinding.region16a.isSensitive = true
        viewBinding.region16b.isSensitive = true
        viewBinding.region16c.isSensitive = true
        viewBinding.region16d.isSensitive = true
    }
}
