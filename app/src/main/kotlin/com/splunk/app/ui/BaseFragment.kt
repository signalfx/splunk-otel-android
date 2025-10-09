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

package com.splunk.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.splunk.app.util.FragmentAnimation
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.navigation.extension.navigation

abstract class BaseFragment<T : ViewBinding> : Fragment() {

    private val activity: MainActivity
        get() = requireActivity() as MainActivity

    private var viewBindingInternal: T? = null

    protected abstract val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> T

    protected val viewBinding: T
        get() = viewBindingInternal ?: error("Must be called after createView()")

    abstract val titleRes: Int

    open val subtitleRes: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewBindingInternal = viewBindingCreator(inflater, container, false)
        return viewBindingInternal?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SplunkRum.instance.navigation.track(getString(titleRes))
    }

    override fun onDestroyView() {
        viewBindingInternal = null
        super.onDestroyView()
    }

    fun navigateTo(fragment: BaseFragment<*>, animation: FragmentAnimation? = null, args: Bundle? = null) {
        args?.let {
            fragment.arguments = it
        }
        activity.navigateTo(fragment, animation)
    }
}
