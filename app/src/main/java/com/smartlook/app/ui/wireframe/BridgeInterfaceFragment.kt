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

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import com.smartlook.app.R
import com.smartlook.app.databinding.FragmentBridgeInterfaceBinding
import com.smartlook.app.ui.BaseFragment

class BridgeInterfaceFragment : BaseFragment<FragmentBridgeInterfaceBinding>() {

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentBridgeInterfaceBinding
        get() = FragmentBridgeInterfaceBinding::inflate

    override val titleRes: Int = R.string.bridge_interface_title

    private val animator by lazy { ObjectAnimator.ofFloat(viewBinding.rectangle, "rotation", 0f, 360f) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animator.interpolator = LinearInterpolator()
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.repeatMode = ObjectAnimator.RESTART
        animator.duration = 1000L
        animator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        animator.cancel()
    }
}
