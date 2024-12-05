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

// TODO out of scope for GA
//package com.smartlook.app.bridge
//
//import android.animation.ArgbEvaluator
//import android.graphics.Rect
//import android.view.View
//import com.smartlook.app.view.bridge.TomasElement
//import com.smartlook.app.view.bridge.TomasView
//import com.smartlook.sdk.bridge.model.BridgeFrameworkInfo
//import com.smartlook.sdk.bridge.model.BridgeInterface
//import com.smartlook.sdk.bridge.model.BridgeWireframe
//import com.smartlook.sdk.common.utils.Colors
//import com.smartlook.sdk.common.utils.extensions.safeSubmit
//import java.util.concurrent.Executors
//
//class TomasBridgeInterface : BridgeInterface {
//
//    private val executor = Executors.newCachedThreadPool()
//    private val classes = listOf(TomasView::class.java)
//
//    override var isRecordingAllowed: Boolean = true
//        private set
//
//    override fun obtainFrameworkInfo(callback: (BridgeFrameworkInfo?) -> Unit) {
//        callback(
//            BridgeFrameworkInfo(
//                framework = "TomasBridgeInterface",
//                frameworkPluginVersion = "1.0",
//                frameworkVersion = "0.1"
//            )
//        )
//    }
//
//    override fun obtainWireframeRootClasses(): List<Class<out View>> {
//        return classes
//    }
//
//    override fun obtainWireframeData(instance: View, callback: (BridgeWireframe?) -> Unit) {
//        if (instance !is TomasView) {
//            callback(null)
//            return
//        }
//
//        instance.listener = tomasViewListener
//
//        executor.safeSubmit {
//            simulatePerformanceDemanding()
//
//            val bridgeWireframe = extractBridgeWireframe(instance)
//            callback(bridgeWireframe)
//        }
//    }
//
//    private fun simulatePerformanceDemanding() {
//        val delay = (2 + Math.random() * 3).toLong()
//        Thread.sleep(delay)
//    }
//
//    private fun extractBridgeWireframe(view: TomasView): BridgeWireframe {
//        val subviews = ArrayList<BridgeWireframe.View>()
//
//        for (element in view.elements)
//            subviews += when (element) {
//                is TomasElement.Circle ->
//                    describeCircle(element)
//                is TomasElement.Rectangle ->
//                    describeRectangle(element)
//                is TomasElement.GradientRectangle ->
//                    describeGradientRectangle(element)
//            }
//
//        return BridgeWireframe(
//            root = BridgeWireframe.View(
//                id = "_bridgeRoot",
//                name = null,
//                rect = Rect(0, 0, view.width, view.height),
//                type = null,
//                typename = "TomasRootElement",
//                hasFocus = false,
//                offset = null,
//                alpha = 1f,
//                isSensitive = false,
//                skeletons = null,
//                foregroundSkeletons = null,
//                subviews = subviews
//            ),
//            width = view.width,
//            height = view.height
//        )
//    }
//
//    private fun describeCircle(element: TomasElement.Circle): BridgeWireframe.View {
//        val rect = Rect(element.x - element.radius, element.y - element.radius, element.x + element.radius, element.y + element.radius)
//
//        return BridgeWireframe.View(
//            id = "_${element.hashCode()}",
//            name = null,
//            rect = rect,
//            type = null,
//            typename = "Circle",
//            hasFocus = false,
//            offset = null,
//            alpha = 1f,
//            isSensitive = element.isSensitive,
//            skeletons = listOf(
//                BridgeWireframe.View.Skeleton(
//                    colors = Colors(element.color),
//                    radius = 0,
//                    type = null,
//                    rect = Rect(rect),
//                    flags = null,
//                    isOpaque = false
//                )
//            ),
//            foregroundSkeletons = null,
//            subviews = null
//        )
//    }
//
//    private fun describeRectangle(element: TomasElement.Rectangle): BridgeWireframe.View {
//        return BridgeWireframe.View(
//            id = "_${element.hashCode()}",
//            name = null,
//            rect = Rect(element.rect),
//            type = null,
//            typename = "Rectangle",
//            hasFocus = false,
//            offset = null,
//            alpha = 1f,
//            isSensitive = element.isSensitive,
//            skeletons = listOf(
//                BridgeWireframe.View.Skeleton(
//                    colors = Colors(element.color),
//                    radius = 0,
//                    type = null,
//                    rect = Rect(element.rect),
//                    flags = null,
//                    isOpaque = true
//                )
//            ),
//            foregroundSkeletons = null,
//            subviews = null
//        )
//    }
//
//    private fun describeGradientRectangle(element: TomasElement.GradientRectangle): BridgeWireframe.View {
//        val p25 = ARGB_EVALUATOR.evaluate(0.25f, element.topLeftColor, element.bottomRightColor) as Int
//        val p50 = ARGB_EVALUATOR.evaluate(0.5f, element.topLeftColor, element.bottomRightColor) as Int
//        val p75 = ARGB_EVALUATOR.evaluate(0.75f, element.topLeftColor, element.bottomRightColor) as Int
//
//        val colors = Colors(3, 3)
//        colors[0, 0] = element.topLeftColor
//        colors[1, 0] = p25
//        colors[2, 0] = p50
//        colors[0, 1] = p25
//        colors[1, 1] = p50
//        colors[2, 1] = p50
//        colors[0, 2] = p50
//        colors[1, 2] = p75
//        colors[2, 2] = element.bottomRightColor
//
//        return BridgeWireframe.View(
//            id = "_${element.hashCode()}",
//            name = null,
//            rect = Rect(element.rect),
//            type = null,
//            typename = "Rectangle",
//            hasFocus = false,
//            offset = null,
//            alpha = 1f,
//            isSensitive = element.isSensitive,
//            skeletons = listOf(
//                BridgeWireframe.View.Skeleton(
//                    colors = colors,
//                    radius = 0,
//                    type = null,
//                    rect = Rect(element.rect),
//                    flags = null,
//                    isOpaque = true
//                )
//            ),
//            foregroundSkeletons = null,
//            subviews = null
//        )
//    }
//
//    private val tomasViewListener = object : TomasView.Listener {
//        override fun onTransitionChanged(isRunning: Boolean) {
//            isRecordingAllowed = !isRunning
//        }
//    }
//
//    private companion object {
//        val ARGB_EVALUATOR = ArgbEvaluator()
//    }
//}
