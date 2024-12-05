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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cisco.android.rum.integration.recording.api.extension.sessionReplay
import com.smartlook.app.ui.compose.theme.BasicTheme
import com.smartlook.app.view.SimpleGLSurfaceView

class SurfaceComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Content()
        }
    }
}

@Composable
private fun Content() {
    BasicTheme {
        Box {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .verticalScroll(
                        state = scrollState
                    )
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = 20.dp),
                    factory = ::SimpleGLSurfaceView
                )
                Text(
                    modifier = Modifier
                        .sessionReplay(
                            id = "label",
                            isSensitive = true
                        )
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            top = 20.dp
                        )
                        .height(1500.dp),
                    text = "↓↓↓ Scroll down ↓↓↓"
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {}
                ) {
                    Text(text = "Click")
                }
            }
        }
    }
}
