/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.app.ui.anr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.splunk.app.R
import com.splunk.app.databinding.FragmentAnrBinding
import com.splunk.app.ui.BaseFragment

/**
 * Fragment for triggering ANR (Application Not Responding) events at various durations to test the
 * configurable ANR polling interval.
 */
class AnrFragment : BaseFragment<FragmentAnrBinding>() {

    override val titleRes: Int = R.string.anr_title

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentAnrBinding
        get() = FragmentAnrBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            anrBlock3s.setOnClickListener { blockMainThread(3_000L) }
            anrBlock4s.setOnClickListener { blockMainThread(4_000L) }
            anrBlock5s.setOnClickListener { blockMainThread(5_000L) }
            anrBlock6s.setOnClickListener { blockMainThread(6_000L) }
            anrBlock7s.setOnClickListener { blockMainThread(7_000L) }
            anrBlock8s.setOnClickListener { blockMainThread(8_000L) }
            anrBlock10s.setOnClickListener { blockMainThread(10_000L) }
            anrBlock11s.setOnClickListener { blockMainThread(11_000L) }
            anrBlock12s.setOnClickListener { blockMainThread(12_000L) }
            anrBlock15s.setOnClickListener { blockMainThread(15_000L) }
            anrBlock16s.setOnClickListener { blockMainThread(16_000L) }
            anrBlock18s.setOnClickListener { blockMainThread(18_000L) }
            anrBlock20s.setOnClickListener { blockMainThread(20_000L) }
        }
    }

    private fun blockMainThread(durationMs: Long) {
        try {
            Thread.sleep(durationMs)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }
}
