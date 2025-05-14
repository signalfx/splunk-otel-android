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

package com.splunk.app.screens

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.PerformException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.splunk.app.lib.ResourceType
import com.splunk.app.lib.TestConstants
import com.splunk.app.lib.resType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.hamcrest.Matcher

/**
 * Utility function to call a specific screen. Instead of instantiating page objects,
 * simply call the screen<ScreenClass> { ... }.
 *
 * Example: screen<MainScreen> { myButton.click() }
 */
inline fun <reified T : Screen> screen(init: T.() -> Unit): T? = T::class.java.getDeclaredConstructor().newInstance().apply { init() }

open class Screen {

    /**
     * This utility function simplifies the need to call a specific resource.
     * onView(withId(...)) or onView(withText(...)) simply becomes view(...).
     */
    fun <T> view(element: T): Matcher<View> = when (element) {
        is Int -> when (element.resType) {
            ResourceType.RESOURCE_ID -> withId(element)
            ResourceType.RESOURCE_STRING -> withText(element)
        }

        is String -> withText(element)
        else -> throw NoSuchElementException()
    }

    /**
     * This safe click function will attempt to scroll to the view prior to clicking.
     * If the view is already visible, it will bypass the exception and click the item.
     */
    fun Matcher<View>.click(): ViewInteraction {
        scrollFirst(this)
        return onView(this).perform(ViewActions.click())
    }

    fun Matcher<View>.replaceText(text: String): ViewInteraction {
        scrollFirst(this)
        return onView(this).perform(ViewActions.replaceText(text))
    }

    /**
     * A helper function to silently handle scrolling errors which are thrown on some elements that
     * cannot be scrolled on, for example toolbars.
     */
    private fun scrollFirst(matcher: Matcher<View>) {
        try {
            onView(matcher).perform(ViewActions.scrollTo())
        } catch (exception: PerformException) {
            // Do nothing, view already visible or cannot be scrolled on
        }
    }

    /** Default back option. */
    fun goBack() = Espresso.pressBack()

    /**
     * Waits for a [Matcher<View>].
     */
    fun waitForView(
        matcher: Matcher<View>,
        timeout: Long = TestConstants.DEFAULT_NETWORK_TIMEOUT,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS
    ): Boolean {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeUnit.toMillis(timeout)
        val latch = CountDownLatch(1)

        val viewAssertion = ViewAssertion { view, noViewFoundException ->
            if (noViewFoundException != null) {
                throw noViewFoundException
            }

            if (ViewMatchers.isDisplayed().matches(view)) {
                latch.countDown()
            }
        }

        do {
            try {
                onView(matcher).check(viewAssertion)

                if (latch.await(50, TimeUnit.MILLISECONDS)) {
                    return true // Found the view and it's visible
                }
            } catch (e: NoMatchingViewException) {
                // View not found, sleep for a short duration before retrying
                Thread.sleep(50)
            }

            if (System.currentTimeMillis() > endTime) {
                throw AssertionError("View with matcher $matcher not found within the timeout")
            }
        } while (true)
    }

    /** Selects an item from a RecyclerView at a position */
    fun selectItemFromRecyclerView(recyclerView: Matcher<View>, position: Int) {
        onView(recyclerView)
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    ViewActions.click()
                )
            )
    }

    /** Wait for some time to allow any MELT data to be sent. */
    fun waitForSendingData(
        timeout: Long = TestConstants.DEFAULT_MELT_TIMEOUT
    ) {
        Thread.sleep(timeout)
    }
}
