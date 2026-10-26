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

package com.splunk.rum.integration.agent.api.internal

import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.integration.agent.api.resource.AgentResource
import com.splunk.rum.integration.agent.internal.user.IUserManager
import com.splunk.rum.integration.agent.internal.user.resolvePendingUserId
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.data.DelegatingSpanData
import io.opentelemetry.sdk.trace.data.SpanData
import java.util.concurrent.atomic.AtomicReference

/** Resolves the app installation ID and anonymous user ID, which are read from storage after installation. */
internal class StoredIdentityAttributes(
    private val appInstallationId: AtomicReference<String>,
    private val userManager: IUserManager
) {

    /** Resolves the pending user ID of a span being exported. May wait for storage. */
    fun applyTo(span: SpanData): SpanData {
        val attributes = userManager.resolvePendingUserId(span.attributes, wait = true)

        return if (attributes === span.attributes) {
            span
        } else {
            object : DelegatingSpanData(span) {
                override fun getAttributes(): Attributes = attributes
            }
        }
    }

    /** Never waits, so a value that is not read yet stays pending. */
    fun resourceAttributesIfRead(resource: Resource): Attributes {
        val attributes = resource.attributes
        if (attributes.get(APP_INSTALLATION_ID_KEY) != IAgentStorage.PENDING_APP_INSTALLATION_ID) {
            return attributes
        }

        val id = appInstallationId.get() ?: return attributes
        return attributes.toBuilder().put(APP_INSTALLATION_ID_KEY, id).build()
    }

    /** Never waits, so a value that is not read yet stays pending. */
    fun spanAttributesIfRead(attributes: Attributes): Attributes =
        userManager.resolvePendingUserId(attributes, wait = false)

    private companion object {
        val APP_INSTALLATION_ID_KEY = AttributeKey.stringKey(AgentResource.KEY_APP_INSTALLATION_ID)
    }
}
