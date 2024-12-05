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

package com.smartlook.app.ui.menu

import android.content.Intent
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cisco.android.rum.integration.agent.api.CiscoRUMAgent
import com.cisco.android.rum.integration.recording.api.extension.isSensitive
import com.cisco.android.rum.integration.recording.api.extension.sessionReplay
import com.smartlook.app.R
import com.smartlook.app.databinding.FragmentMenuBinding
import com.smartlook.app.extension.screenSize
import com.smartlook.app.ui.BaseFragment
import com.smartlook.app.ui.compose.CameraComposeActivity
import com.smartlook.app.ui.compose.DrawOrderComposeActivity
import com.smartlook.app.ui.compose.DrawRecompositionComposeActivity
import com.smartlook.app.ui.compose.ListComposeActivity
import com.smartlook.app.ui.compose.MeasureRecompositionComposeActivity
import com.smartlook.app.ui.compose.SurfaceComposeActivity
import com.smartlook.app.ui.compose.TextFieldComposeActivity
import com.smartlook.app.ui.compose.VideoComposeActivity
import com.smartlook.app.ui.compose.ViewDrawOrderComposeActivity
import com.smartlook.app.ui.compose.WebViewComposeActivity
import com.smartlook.app.ui.httpurlconnection.HttpURLConnectionFragment
import com.smartlook.app.ui.interaction.FocusActivity
import com.smartlook.app.ui.okhttp.OkHttpFragment
import com.smartlook.app.ui.screenshot.AnimationFragment
import com.smartlook.app.ui.screenshot.ScreenshotRegionsFragment
import com.smartlook.app.ui.screenshot.ScreenshotViewsFragment
import com.smartlook.app.ui.wireframe.BridgeInterfaceFragment
import com.smartlook.app.ui.wireframe.CollapsingLayoutFragment
import com.smartlook.app.ui.wireframe.EmptyActivity
import com.smartlook.app.ui.wireframe.ListFragment
import com.smartlook.app.ui.wireframe.WebViewFragment
import com.smartlook.app.ui.wireframe.WireframeViewsFragment
import com.smartlook.app.util.FragmentAnimation
import com.smartlook.sdk.common.utils.extensions.toRect

class MenuFragment : BaseFragment<FragmentMenuBinding>() {

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentMenuBinding
        get() = FragmentMenuBinding::inflate

    override val titleRes: Int = R.string.menu_title

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.screenshotViews.setOnClickListener(onClickListener)
        viewBinding.screenshotRegions.setOnClickListener(onClickListener)
        viewBinding.screenshotMasks.setOnClickListener(onClickListener)
        viewBinding.screenshotFragmentAnimationNone.setOnClickListener(onClickListener)
        viewBinding.screenshotFragmentAnimationFade1.setOnClickListener(onClickListener)
        viewBinding.screenshotFragmentAnimationFade2.setOnClickListener(onClickListener)
        viewBinding.screenshotFragmentAnimationFade.setOnClickListener(onClickListener)
        viewBinding.screenshotFragmentAnimationTranslate1.setOnClickListener(onClickListener)
        viewBinding.screenshotFragmentAnimationTranslate2.setOnClickListener(onClickListener)
        viewBinding.screenshotFragmentAnimationTranslate.setOnClickListener(onClickListener)
        viewBinding.interactionsFocus.setOnClickListener(onClickListener)
        viewBinding.wireframeViews.setOnClickListener(onClickListener)
        viewBinding.wireframeCollapsingLayout.setOnClickListener(onClickListener)
        viewBinding.wireframeOrdering.setOnClickListener(onClickListener)
        viewBinding.wireframeList.setOnClickListener(onClickListener)
        viewBinding.wireframeWebView.setOnClickListener(onClickListener)
        viewBinding.bridgeInterface.setOnClickListener(onClickListener)
        viewBinding.composeList.setOnClickListener(onClickListener)
        viewBinding.composeDrawOrder.setOnClickListener(onClickListener)
        viewBinding.composeViewDrawOrder.setOnClickListener(onClickListener)
        viewBinding.composeDrawRecomposition.setOnClickListener(onClickListener)
        viewBinding.composeMeasureRecomposition.setOnClickListener(onClickListener)
        viewBinding.composeSurface.setOnClickListener(onClickListener)
        viewBinding.composeCamera.setOnClickListener(onClickListener)
        viewBinding.composeVideo.setOnClickListener(onClickListener)
        viewBinding.composeTextField.setOnClickListener(onClickListener)
        viewBinding.composeWebView.setOnClickListener(onClickListener)
        viewBinding.crashReportsIllegal.setOnClickListener(onClickListener)
        viewBinding.crashReportsMainThread.setOnClickListener(onClickListener)
        viewBinding.crashReportsInBackground.setOnClickListener(onClickListener)
        viewBinding.crashReportsNoAppCode.setOnClickListener(onClickListener)
        viewBinding.crashReportsNoStacktrace.setOnClickListener(onClickListener)
        viewBinding.crashReportsOutOfMemoryError.setOnClickListener(onClickListener)
        viewBinding.crashReportsWithChainedExceptions.setOnClickListener(onClickListener)
        viewBinding.crashReportsNull.setOnClickListener(onClickListener)
        viewBinding.anrEvent.setOnClickListener(onClickListener)
        viewBinding.okhttpSampleCalls.setOnClickListener(onClickListener)
        viewBinding.httpurlconnection.setOnClickListener(onClickListener)
        viewBinding.wireframeViews.isSensitive = true

        // viewBinding.screenshotViews.smartlookId = "screenshot_views_button"
    }

    private fun toggleMasks() {
        CiscoRUMAgent.instance.sessionReplay.recordingMask = if (CiscoRUMAgent.instance.sessionReplay.recordingMask == null) {
            val screenSize = requireContext().screenSize
            val w = screenSize.width
            val h = screenSize.height

            com.cisco.android.rum.integration.recording.api.RecordingMask(
                listOf(
                    com.cisco.android.rum.integration.recording.api.RecordingMask.Element(
                        Rect(0, 0, w, h),
                        com.cisco.android.rum.integration.recording.api.RecordingMask.Element.Type.COVERING
                    ),
                    com.cisco.android.rum.integration.recording.api.RecordingMask.Element(
                        RectF(w * 0.1f, w * 0.1f, w * 0.9f, h - w * 0.1f).toRect(),
                        com.cisco.android.rum.integration.recording.api.RecordingMask.Element.Type.ERASING
                    ),
                    com.cisco.android.rum.integration.recording.api.RecordingMask.Element(
                        RectF(w * 0.2f, w * 0.2f, w * 0.45f, w * 0.5f).toRect(),
                        com.cisco.android.rum.integration.recording.api.RecordingMask.Element.Type.COVERING
                    ),
                    com.cisco.android.rum.integration.recording.api.RecordingMask.Element(
                        RectF(w * 0.55f, w * 0.2f, w * 0.8f, w * 0.5f).toRect(),
                        com.cisco.android.rum.integration.recording.api.RecordingMask.Element.Type.COVERING
                    ),
                    com.cisco.android.rum.integration.recording.api.RecordingMask.Element(
                        RectF(w * 0.05f, w * 0.3f, w * 0.95f, w * 0.4f).toRect(),
                        com.cisco.android.rum.integration.recording.api.RecordingMask.Element.Type.ERASING
                    ),
                    com.cisco.android.rum.integration.recording.api.RecordingMask.Element(
                        RectF(w * 0.475f, w * 0.15f, w * 0.525f, w * 0.55f).toRect(),
                        com.cisco.android.rum.integration.recording.api.RecordingMask.Element.Type.COVERING
                    ),
                    com.cisco.android.rum.integration.recording.api.RecordingMask.Element(
                        RectF(w * 0.4f, w * 0.25f, w * 0.6f, w * 0.45f).toRect(),
                        com.cisco.android.rum.integration.recording.api.RecordingMask.Element.Type.ERASING
                    )
                )
            )
        } else
            null
    }

    private val onClickListener = View.OnClickListener {
        when (it.id) {
            viewBinding.screenshotViews.id ->
                navigateTo(ScreenshotViewsFragment(), FragmentAnimation.FADE)
            viewBinding.screenshotRegions.id ->
                navigateTo(ScreenshotRegionsFragment(), FragmentAnimation.FADE)
            viewBinding.screenshotMasks.id ->
                toggleMasks()
            viewBinding.screenshotFragmentAnimationNone.id ->
                navigateTo(AnimationFragment())
            viewBinding.screenshotFragmentAnimationFade1.id ->
                navigateTo(AnimationFragment(), FragmentAnimation.SLOW_FADE_1)
            viewBinding.screenshotFragmentAnimationFade2.id ->
                navigateTo(AnimationFragment(), FragmentAnimation.SLOW_FADE_2)
            viewBinding.screenshotFragmentAnimationFade.id ->
                navigateTo(AnimationFragment(), FragmentAnimation.SLOW_FADE)
            viewBinding.screenshotFragmentAnimationTranslate1.id ->
                navigateTo(AnimationFragment(), FragmentAnimation.SLOW_TRANSLATE_1)
            viewBinding.screenshotFragmentAnimationTranslate2.id ->
                navigateTo(AnimationFragment(), FragmentAnimation.SLOW_TRANSLATE_2)
            viewBinding.screenshotFragmentAnimationTranslate.id ->
                navigateTo(AnimationFragment(), FragmentAnimation.SLOW_TRANSLATE)
            viewBinding.wireframeViews.id ->
                navigateTo(WireframeViewsFragment(), FragmentAnimation.FADE)
            viewBinding.wireframeCollapsingLayout.id ->
                navigateTo(CollapsingLayoutFragment(), FragmentAnimation.FADE)
            viewBinding.wireframeOrdering.id ->
                startActivity(Intent(requireContext(), EmptyActivity::class.java))
            viewBinding.wireframeList.id ->
                navigateTo(ListFragment(), FragmentAnimation.FADE)
            viewBinding.interactionsFocus.id ->
                startActivity(Intent(requireContext(), FocusActivity::class.java))
            viewBinding.composeList.id ->
                startActivity(Intent(requireContext(), ListComposeActivity::class.java))
            viewBinding.composeDrawOrder.id ->
                startActivity(Intent(requireContext(), DrawOrderComposeActivity::class.java))
            viewBinding.composeViewDrawOrder.id ->
                startActivity(Intent(requireContext(), ViewDrawOrderComposeActivity::class.java))
            viewBinding.composeDrawRecomposition.id ->
                startActivity(Intent(requireContext(), DrawRecompositionComposeActivity::class.java))
            viewBinding.composeMeasureRecomposition.id ->
                startActivity(Intent(requireContext(), MeasureRecompositionComposeActivity::class.java))
            viewBinding.composeSurface.id ->
                startActivity(Intent(requireContext(), SurfaceComposeActivity::class.java))
            viewBinding.composeCamera.id ->
                startActivity(Intent(requireContext(), CameraComposeActivity::class.java))
            viewBinding.composeVideo.id ->
                startActivity(Intent(requireContext(), VideoComposeActivity::class.java))
            viewBinding.composeTextField.id ->
                startActivity(Intent(requireContext(), TextFieldComposeActivity::class.java))
            viewBinding.composeWebView.id ->
                startActivity(Intent(requireContext(), WebViewComposeActivity::class.java))
            viewBinding.wireframeWebView.id ->
                navigateTo(WebViewFragment(), FragmentAnimation.FADE)
            viewBinding.bridgeInterface.id ->
                navigateTo(BridgeInterfaceFragment(), FragmentAnimation.FADE)
            viewBinding.crashReportsIllegal.id ->
                throw IllegalArgumentException("Illegal Argument Exception Thrown!")
            viewBinding.crashReportsMainThread.id ->
                throw RuntimeException("Crashing on main thread")
            viewBinding.crashReportsInBackground.id ->
                Thread { throw RuntimeException("Attempt to crash background thread") }.start()
            viewBinding.crashReportsNoAppCode.id -> {
                val e = java.lang.RuntimeException("No Application Code")
                e.stackTrace = arrayOf(
                    StackTraceElement("android.fake.Crash", "crashMe", "NotARealFile.kt", 12),
                    StackTraceElement("android.fake.Class", "foo", "NotARealFile.kt", 34),
                    StackTraceElement("android.fake.Main", "main", "NotARealFile.kt", 56)
                )
                throw e
            }
            viewBinding.crashReportsNoStacktrace.id -> {
                val e = java.lang.RuntimeException("No Stack Trace")
                e.stackTrace = arrayOfNulls(0)
                throw e
            }
            viewBinding.crashReportsOutOfMemoryError.id -> {
                val e = OutOfMemoryError("out of memory")
                e.stackTrace = arrayOfNulls(0)
                throw e
            }
            viewBinding.crashReportsWithChainedExceptions.id -> {
                try {
                    throw NullPointerException("Simulated error in exception 1")
                } catch (e: NullPointerException) {
                    throw IllegalArgumentException("Simulated error in exception 2", e)
                }
            }
            viewBinding.crashReportsNull.id ->
                throw NullPointerException("I am null!")
            viewBinding.anrEvent.id -> {
                try {
                    Thread.sleep(6000)
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
            viewBinding.okhttpSampleCalls.id ->
                navigateTo(OkHttpFragment(), FragmentAnimation.FADE)
            viewBinding.httpurlconnection.id ->
                navigateTo(HttpURLConnectionFragment(), FragmentAnimation.FADE)
        }
    }
}
