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

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cisco.android.rum.integration.recording.api.extension.sessionReplay
import com.smartlook.app.ui.compose.theme.BasicTheme

class ViewDrawOrderComposeActivity : ComponentActivity() {

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
        TestSkeletonOrder()
    }
}

@Composable
private fun TestSkeletonOrder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Rectangles(
                modifier = Modifier
                    .padding(top = 10.dp),
                useSmartlookModifier = true
            )

            Rectangles(
                modifier = Modifier
                    .padding(top = 10.dp),
                useSmartlookModifier = false
            )
        }
    }
}

@Composable
private fun Rectangles(modifier: Modifier, useSmartlookModifier: Boolean) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(
                    width = 100.dp,
                    height = 100.dp
                )
                .background(
                    color = Color.Red
                )
        )
        AndroidView(
            factory = {
                View(it).apply { background = ColorDrawable(android.graphics.Color.GREEN) }
            },
            modifier = Modifier
                .matchParentSize()
                .padding(
                    horizontal = 10.dp,
                    vertical = 10.dp
                ).whether({ useSmartlookModifier }) {
                    sessionReplay()
                }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                )
                .background(
                    color = Color.Blue
                )
        )

        if (useSmartlookModifier)
            Text(
                text = "Smartlook",
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.Center)
            )
    }
}

private fun <T> T.whether(predicate: () -> Boolean, action: T.() -> T): T {
    return if (predicate()) action() else this
}
