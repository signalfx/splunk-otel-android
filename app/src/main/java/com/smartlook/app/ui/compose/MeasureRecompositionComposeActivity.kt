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

package com.smartlook.app.ui.compose

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.os.postDelayed
import com.smartlook.sdk.common.utils.extensions.contentView

class MeasureRecompositionComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var lines by remember { mutableStateOf(2) }

            Handler(Looper.getMainLooper()).postDelayed(2000) {
                lines = 30
            }

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .background(Color.Red)
            ) {
                (0..lines).forEach {
                    Box(
                        modifier = Modifier
                            .padding(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Blue)
                                .fillMaxWidth()
                                .height(5.dp)
                        )
                    }
                }
            }
        }

        contentView?.getChildAt(0)?.setBackgroundColor(0xff00ff00.toInt())
    }
}
