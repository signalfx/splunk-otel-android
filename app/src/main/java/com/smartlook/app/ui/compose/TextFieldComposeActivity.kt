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

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartlook.app.ui.compose.theme.BasicTheme

class TextFieldComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApp()
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, heightDp = 320)
@Composable
private fun MyApp() {
    BasicTheme {
        Column(
            modifier = Modifier
                .padding(20.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "Single line"
            )

            var singleLineText by remember { mutableStateOf("") }

            TextField(
                value = singleLineText,
                modifier = Modifier
                    .padding(
                        top = 20.dp
                    )
                    .border(
                        width = 2.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(
                            size = 10.dp
                        )
                    )
                    .background(Color.Transparent)
                    .fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter a text",
                    )
                },
                onValueChange = {
                    singleLineText = it
                },
                textStyle = MaterialTheme.typography.body1,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    placeholderColor = Color.Gray,
                    cursorColor = Color.Blue
                ),
                singleLine = true
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 20.dp
                    ),
                text = "Multi line - fixed height"
            )

            var multiLineText by remember { mutableStateOf("") }

            TextField(
                value = multiLineText,
                modifier = Modifier
                    .padding(
                        top = 20.dp
                    )
                    .border(
                        width = 2.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(
                            size = 10.dp
                        )
                    )
                    .background(Color.Transparent)
                    .height(100.dp)
                    .fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter a text",
                    )
                },
                onValueChange = {
                    multiLineText = it
                },
                textStyle = MaterialTheme.typography.body1,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    placeholderColor = Color.Gray,
                    cursorColor = Color.Blue
                )
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 20.dp
                    ),
                text = "Multi line - wrap height"
            )

            var multiLineTextWrapHeight by remember { mutableStateOf("") }

            TextField(
                value = multiLineTextWrapHeight,
                modifier = Modifier
                    .padding(
                        top = 20.dp
                    )
                    .border(
                        width = 2.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(
                            size = 10.dp
                        )
                    )
                    .background(Color.Transparent)
                    .fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter a text",
                    )
                },
                onValueChange = {
                    multiLineTextWrapHeight = it
                },
                textStyle = MaterialTheme.typography.body1,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    placeholderColor = Color.Gray,
                    cursorColor = Color.Blue
                )
            )
        }
    }
}
