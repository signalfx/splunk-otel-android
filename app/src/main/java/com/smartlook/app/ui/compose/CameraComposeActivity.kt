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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.smartlook.app.ui.compose.theme.BasicTheme
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraComposeActivity : ComponentActivity() {

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
        val context = LocalContext.current

        var grantedPermission by remember { mutableStateOf(false) }

        val requestPermission = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { grantedPermission = it }
        )

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                grantedPermission = true
            else
                requestPermission.launch(Manifest.permission.CAMERA)
        }

        if (grantedPermission)
            Box {
                Camera()
            }
    }
}

@Composable
private fun Camera() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder()
        .build()

    val previewView = remember { PreviewView(context) }
    previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE // TODO Investigate compatibility with PERFORMANCE option

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()

        try {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: IllegalArgumentException) {
            Log.e("Camera", "No camera available", e)
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxSize()
    )
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider {
    return suspendCoroutine { continuation ->
        val provider = ProcessCameraProvider.getInstance(this)
        val listener = { continuation.resume(provider.get()) }
        val executor = ContextCompat.getMainExecutor(this)
        provider.addListener(listener, executor)
    }
}
