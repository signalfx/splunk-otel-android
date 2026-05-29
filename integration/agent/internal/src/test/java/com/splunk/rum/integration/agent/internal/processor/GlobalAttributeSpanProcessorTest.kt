/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.agent.internal.processor

import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class GlobalAttributeSpanProcessorTest {

    @Test
    fun `onStart sets initial global attributes on span`() {
        val attributes = MutableAttributes(
            Attributes.of(AttributeKey.stringKey("app.version"), "1.0")
        )
        val processor = GlobalAttributeSpanProcessor(attributes)
        val span = mock(ReadWriteSpan::class.java)

        processor.onStart(Context.root(), span)

        verify(span).setAttribute(AttributeKey.stringKey("app.version"), "1.0")
    }

    @Test
    fun `onStart reflects attributes added after processor creation`() {
        val attributes = MutableAttributes()
        val processor = GlobalAttributeSpanProcessor(attributes)

        attributes["user.id"] = "user-123"
        attributes["account.id"] = "acct-456"

        val span = mock(ReadWriteSpan::class.java)
        processor.onStart(Context.root(), span)

        verify(span).setAttribute(AttributeKey.stringKey("user.id"), "user-123")
        verify(span).setAttribute(AttributeKey.stringKey("account.id"), "acct-456")
    }

    @Test
    fun `onStart reflects attributes modified after processor creation`() {
        val attributes = MutableAttributes(
            Attributes.of(AttributeKey.stringKey("env"), "staging")
        )
        val processor = GlobalAttributeSpanProcessor(attributes)

        attributes["env"] = "production"

        val span = mock(ReadWriteSpan::class.java)
        processor.onStart(Context.root(), span)

        verify(span).setAttribute(AttributeKey.stringKey("env"), "production")
    }

    @Test
    fun `onStart reflects attributes removed after processor creation`() {
        val attributes = MutableAttributes(
            Attributes.of(
                AttributeKey.stringKey("keep"),
                "yes",
                AttributeKey.stringKey("remove"),
                "gone"
            )
        )
        val processor = GlobalAttributeSpanProcessor(attributes)

        attributes.remove("remove")

        val span = mock(ReadWriteSpan::class.java)
        processor.onStart(Context.root(), span)

        verify(span).setAttribute(AttributeKey.stringKey("keep"), "yes")
        verify(span, never()).setAttribute(AttributeKey.stringKey("remove"), "gone")
    }

    @Test
    fun `onStart skips internal global attributes`() {
        val internalAttributeKey = AttributeKey.booleanKey("splunk.agent.internal.sessionReplay.hideWireframe")
        val attributes = MutableAttributes(
            Attributes.of(
                AttributeKey.stringKey("app.version"),
                "1.0",
                internalAttributeKey,
                true
            )
        )
        val processor = GlobalAttributeSpanProcessor(attributes)
        val span = mock(ReadWriteSpan::class.java)

        processor.onStart(Context.root(), span)

        verify(span).setAttribute(AttributeKey.stringKey("app.version"), "1.0")
        verify(span, never()).setAttribute(internalAttributeKey, true)
    }

    @Test
    fun `onStart with empty attributes sets nothing harmful`() {
        val attributes = MutableAttributes()
        val processor = GlobalAttributeSpanProcessor(attributes)
        val span = mock(ReadWriteSpan::class.java)

        processor.onStart(Context.root(), span)

        verifyNoInteractions(span)
    }

    @Test
    fun `isStartRequired returns true`() {
        val processor = GlobalAttributeSpanProcessor(MutableAttributes())
        assertTrue(processor.isStartRequired())
    }

    @Test
    fun `isEndRequired returns false`() {
        val processor = GlobalAttributeSpanProcessor(MutableAttributes())
        assertFalse(processor.isEndRequired())
    }
}
