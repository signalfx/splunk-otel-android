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

@file:OptIn(ExperimentalMaterialApi::class)

package com.smartlook.app.ui.compose

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.LayoutDirection
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.layoutDirection
import androidx.core.widget.NestedScrollView
import com.cisco.android.rum.integration.recording.api.extension.sessionReplay
import com.smartlook.app.ui.compose.theme.BasicTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

class WebViewComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BasicTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            showAsBottomSheet(
                                background = Color.White
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White)
                                ) {
                                    BottomSheetWebViewContent(
                                        modifier = Modifier.fillMaxSize(),
                                        title = "My web View",
                                        url = "https://getapp-test.astropaycard.com/terms-and-conditions/debit-card-terms.html"
                                    )
                                }
                            }
                        }
                    ) {
                        Text(text = "Open WebView in BottomSheet")
                    }
                }
            }
        }
    }
}

// Code below copied from client
@Composable
private fun HeaderInfo(
    title: String,
    canGoForward: Boolean,
    canGoBack: Boolean,
    onClickPrevious: () -> Unit,
    onClickNext: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        onBack?.let {
            IconNavigationBack { it() }
        }
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            color = Color.Black
        )

        Row {
            ArrowIndicator(
                imageVector = Icons.Filled.ChevronLeft,
                isEnable = canGoBack,
                onClick = {
                    onClickPrevious.invoke()
                }
            )

            ArrowIndicator(
                imageVector = Icons.Filled.ChevronRight,
                isEnable = canGoForward,
                onClick = {
                    onClickNext.invoke()
                }
            )
        }
    }
}

@Stable
private fun Modifier.readDirection(): Modifier {
    return if (Locale.getDefault().layoutDirection == LayoutDirection.RTL)
        scale(scaleX = -1f, scaleY = 1f)
    else
        this
}

@Composable
private fun ArrowIndicator(
    isEnable: Boolean,
    imageVector: ImageVector,
    onClick: () -> Unit
) {
    var modifier = Modifier
        .readDirection()
        .padding(8.dp)

    modifier = if (isEnable)
        modifier
            .alpha(1f)
            .clickable { onClick.invoke() }
    else
        modifier.alpha(0.5f)

    Icon(
        imageVector = imageVector,
        modifier = modifier,
        tint = Color.Black,
        contentDescription = null,
    )
}

@Composable
private fun IconNavigationBack(
    modifier: Modifier = Modifier,
    navAction: () -> Unit
) {
    IconButton(
        modifier = modifier,
        onClick = navAction
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.readDirection()
        )
    }
}

@Composable
private fun Header(
    title: String,
    progress: Float,
    canGoForward: Boolean,
    canGoBack: Boolean,
    onClickPrevious: () -> Unit,
    onClickNext: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        HeaderInfo(
            title = title,
            onClickPrevious = onClickPrevious,
            onClickNext = onClickNext,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (progress / 100f < 1f) {
            LinearProgressIndicator(
                progress = progress / 100,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color.Black
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BottomSheetWebViewContent(
    modifier: Modifier,
    title: String,
    url: String,
    back: (() -> Unit)? = null
) {
    Column {
        var webView by remember { mutableStateOf<WebView?>(null) }
        var progress: Float by remember { mutableStateOf(0f) }

        val scrollState = rememberScrollState()
        val scope = rememberCoroutineScope()

        val isPreview = LocalInspectionMode.current

        Header(
            title = title,
            onClickNext = {
                webView?.goForward()
            },
            onClickPrevious = {
                webView?.goBack()
            },
            progress = progress,
            canGoBack = webView?.canGoBack() ?: false,
            canGoForward = webView?.canGoForward() ?: false,
            onBack = back
        )

        AndroidView(
            factory = { context ->
                val web = WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            scope.launch { scrollState.scrollTo(0) }
                            super.onPageFinished(view, url)
                        }
                    }

                    if (!isPreview) {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                    }

                    webChromeClient = MyWebViewClient(
                        updateProgress = {
                            progress = it.toFloat()
                        }
                    )

                    webView = this
                }

                NestedScrollView(context).apply {
                    addView(web)
                    web.loadUrl(url)
                }
            },
            modifier = modifier // TODO Investigate sensitivity bounds
                .height(IntrinsicSize.Max)
                .clipToBounds()
                .verticalScroll(scrollState)
                .sessionReplay(
                    id = "WebView"
                )
        )
    }
}

private class MyWebViewClient(val updateProgress: (Int) -> Unit) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        updateProgress(newProgress)
        super.onProgressChanged(view, newProgress)
    }
}

private fun Activity.showAsBottomSheet(
    background: Color,
    scrimColor: Color? = null,
    showDivider: Boolean = true,
    showClose: Boolean = false,
    content: @Composable (
        () -> Unit
    ) -> Unit
) {
    val viewGroup = this.findViewById(android.R.id.content) as ViewGroup
    addContentToView(viewGroup, background, scrimColor, showDivider, showClose, content)
}

private fun addContentToView(
    viewGroup: ViewGroup,
    background: Color,
    scrimColor: Color?,
    showDivider: Boolean = true,
    showClose: Boolean = false,
    content: @Composable (() -> Unit) -> Unit
) {
    viewGroup.addView(
        ComposeView(viewGroup.context).apply {
            setContent {
                BasicTheme {
                    BottomSheetWrapper(viewGroup, this, background, scrimColor, showDivider, showClose, content)
                }
            }
        }
    )
}

@Composable
private fun BottomSheetWrapper(
    parent: ViewGroup,
    composeView: ComposeView,
    background: Color,
    scrimColor: Color?,
    showDivider: Boolean = true,
    showClose: Boolean = false,
    content: @Composable (() -> Unit) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState =
        rememberModalBottomSheetState(
            ModalBottomSheetValue.Hidden,
            confirmStateChange = {
                it != ModalBottomSheetValue.HalfExpanded
            }
        )
    var isSheetOpened by remember { mutableStateOf(false) }

    ModalBottomSheetLayout(
        sheetBackgroundColor = Color.Transparent,
        sheetState = modalBottomSheetState,
        sheetContent = {
            BottomSheetUIWrapper(
                coroutineScope = coroutineScope,
                modalBottomSheetState = modalBottomSheetState,
                background = background,
                showDivider = showDivider,
                showClose = showClose
            ) {
                content {
                    animateHideBottomSheet(coroutineScope, modalBottomSheetState)
                }
            }
        },
        scrimColor = scrimColor ?: ModalBottomSheetDefaults.scrimColor
    ) {}

    // Take action based on hidden state
    LaunchedEffect(modalBottomSheetState.currentValue) {
        when (modalBottomSheetState.currentValue) {
            ModalBottomSheetValue.Hidden -> {
                when {
                    isSheetOpened -> parent.removeView(composeView)
                    else -> {
                        isSheetOpened = true
                        modalBottomSheetState.show()
                    }
                }
            }

            else -> {}
        }
    }

    var isBackEnabled by remember { mutableStateOf(true) }

    BackHandlerExtension(
        enabled = isBackEnabled
    ) {
        animateHideBottomSheet(coroutineScope, modalBottomSheetState)
        isBackEnabled = false
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetUIWrapper(
    coroutineScope: CoroutineScope,
    modalBottomSheetState: ModalBottomSheetState,
    background: Color,
    showDivider: Boolean = true,
    showClose: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 20.dp, topStart = 20.dp))
            .background(background)
    ) {
        Box(Modifier.padding(top = 25.dp)) {
            content()
        }

        if (showDivider) {
            Divider(
                color = Color.Black,
                thickness = 5.dp,
                modifier = Modifier
                    .padding(top = 15.dp)
                    .align(Alignment.TopCenter)
                    .width(40.dp)
                    .clip(RoundedCornerShape(50.dp))
            )
        }

        if (showClose) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd),
                onClick = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }
                }
            ) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.TopEnd),
                    imageVector = Icons.Filled.Close,
                    contentDescription = "",
                    tint = Color.Black
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
private fun animateHideBottomSheet(
    coroutineScope: CoroutineScope,
    modalBottomSheetState: ModalBottomSheetState
) {
    coroutineScope.launch {
        modalBottomSheetState.hide()
    }
}

@Composable
private fun BackHandlerExtension(
    enabled: Boolean = true,
    onBack: () -> Unit
) {

    if (!LocalInspectionMode.current) {
        BackHandler(enabled = enabled, onBack = onBack)
    }
}
