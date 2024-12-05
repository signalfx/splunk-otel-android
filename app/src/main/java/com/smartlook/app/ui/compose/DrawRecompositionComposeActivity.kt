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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cisco.android.rum.integration.recording.api.extension.sessionReplay

class DrawRecompositionComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApp()
        }
    }
}

@Preview
@Composable
private fun MyApp() {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .sessionReplay(
                id = "root"
            )
    ) {
        for (i in 0 until 5)
            CheckboxRow(
                text = "Checkbox $i",
                modifier = Modifier.padding(vertical = 8.dp),
            )
    }
}

@Composable
private fun CheckboxRow(
    text: String,
    modifier: Modifier,
) {
    val selected = rememberSaveable { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.0.dp),
        color = if (selected.value) MaterialTheme.colors.primarySurface else MaterialTheme.colors.secondary,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected.value) MaterialTheme.colors.secondary else MaterialTheme.colors.primary
        ),
        modifier = modifier
            .clip(
                shape = RoundedCornerShape(8.0.dp)
            )
            .clickable(
                onClick = {
                    selected.value = !selected.value
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier.padding(8.dp)
            ) {
                Checkbox(
                    checked = selected.value,
                    onCheckedChange = null
                )
            }
        }
    }
}
