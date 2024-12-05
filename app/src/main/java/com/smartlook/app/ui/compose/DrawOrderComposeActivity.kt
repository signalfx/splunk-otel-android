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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cisco.android.rum.integration.recording.api.extension.sessionReplay
import com.smartlook.app.ui.compose.theme.BasicTheme

class DrawOrderComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApp()
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320)
@Composable
private fun MyApp() {
    BasicTheme {
        TestDrawOrder()
    }
}

@Composable
private fun TestDrawOrder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column {
                Text(
                    text = "sensitive text sensitive text sensitive text sensitive text sensitive text sensitive text sensitive text sensitive text sensitive text sensitive text",
                    modifier = Modifier
                        .padding(15.dp)
                        .sessionReplay(
                            id = "sensitive_text",
                            isSensitive = true
                        )
                        .drawWithContent {
                            drawContent()
                            drawCircle(Color.Green)
                        }
                        .drawBehind {
                            drawRect(Color.Red)
                        },
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "clear text clear text clear text clear text clear text clear text clear text clear text clear text clear text clear text clear text clear text clear text",
                    modifier = Modifier
                        .padding(15.dp)
                        .sessionReplay(
                            id = "clear_text"
                        ),
                    textAlign = TextAlign.Center,
                    color = Color.Blue
                )
            }
        }
    }
}
