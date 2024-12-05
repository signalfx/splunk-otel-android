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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.cisco.android.rum.integration.recording.api.extension.sessionReplay
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.smartlook.app.R
import com.smartlook.app.ui.compose.theme.BasicTheme

class VideoComposeActivity : ComponentActivity() {

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
        VideoView()
    }
}

@Composable
private fun VideoView() {
    val context = LocalContext.current

    val mediaItem = MediaItem.Builder()
        .setUri(RawResourceDataSource.buildRawResourceUri(R.raw.rainy_day))
        .build()

    val player = ExoPlayer.Builder(context)
        .build()

    player.setMediaItem(mediaItem)
    player.repeatMode = Player.REPEAT_MODE_ONE
    player.prepare()

    DisposableEffect(
        AndroidView(
            factory = {
                StyledPlayerView(it).also {
                    it.player = player
                }
            },
            modifier = Modifier
                .aspectRatio(0.764f)
                .sessionReplay()
        )
    ) {
        onDispose { player.release() }
    }
}
