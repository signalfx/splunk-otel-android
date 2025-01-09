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

package com.splunk.app.util

import androidx.annotation.AnimRes
import androidx.annotation.AnimatorRes
import com.splunk.app.R

data class FragmentAnimation(
    @AnimatorRes @AnimRes val enter: Int,
    @AnimatorRes @AnimRes val exit: Int,
    @AnimatorRes @AnimRes val popEnter: Int = 0,
    @AnimatorRes @AnimRes val popExit: Int = 0
) {

    companion object {
        val FADE = FragmentAnimation(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
        val SLOW_FADE = FragmentAnimation(R.animator.slow_fade_in, R.animator.slow_fade_out, R.animator.slow_fade_in, R.animator.slow_fade_out)
        val SLOW_FADE_1 = FragmentAnimation(0, R.animator.slow_fade_out, 0, R.animator.slow_fade_out)
        val SLOW_FADE_2 = FragmentAnimation(R.animator.slow_fade_in, 0, R.animator.slow_fade_in, 0)
        val SLOW_TRANSLATE = FragmentAnimation(R.anim.slow_enter_from_right, R.anim.slow_exit_to_left, R.anim.slow_enter_from_left, R.anim.slow_exit_to_right)
        val SLOW_TRANSLATE_1 = FragmentAnimation(0, R.anim.slow_exit_to_left, 0, R.anim.slow_exit_to_right)
        val SLOW_TRANSLATE_2 = FragmentAnimation(R.anim.slow_enter_from_right, 0, R.anim.slow_enter_from_left, 0)
    }
}
