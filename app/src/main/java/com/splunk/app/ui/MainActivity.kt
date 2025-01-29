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
import androidx.appcompat.app.AppCompatActivity
import com.cisco.android.common.utils.extensions.contentView
import com.splunk.app.databinding.ActivityMainBinding
import com.splunk.app.extension.setCustomAnimations
import com.splunk.app.extension.setSubtitle
import com.splunk.app.ui.menu.MenuFragment
import com.splunk.app.util.FragmentAnimation

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater, contentView, true)

        setSupportActionBar(viewBinding.toolbar)
        viewBinding.toolbar.setNavigationOnClickListener { navigateUp() }

        if (savedInstanceState == null)
            navigateTo(MenuFragment())
        else
            contentView?.post { updateToolbar() }
    }

    override fun onBackPressed() {
        navigateUp()
    }

    fun navigateTo(fragment: BaseFragment<*>, animation: FragmentAnimation? = null) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(animation)
            .replace(viewBinding.container.id, fragment)
            .addToBackStack(fragment.tag ?: fragment.hashCode().toString())
            .commit()

        contentView?.post { updateToolbar() }
    }

    fun navigateUp() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStackImmediate()
            updateToolbar()
        } else
            finish()
    }

    private fun updateToolbar() {
        val isBackButtonVisible = supportFragmentManager.backStackEntryCount > 1
        supportActionBar?.setDisplayHomeAsUpEnabled(isBackButtonVisible)
        supportActionBar?.setDisplayShowHomeEnabled(isBackButtonVisible)

        val fragment = supportFragmentManager.fragments.last() as? BaseFragment<*>
        if (fragment != null) {
            viewBinding.toolbar.setTitle(fragment.titleRes)
            viewBinding.toolbar.setSubtitle(fragment.subtitleRes)
        }
    }
}
