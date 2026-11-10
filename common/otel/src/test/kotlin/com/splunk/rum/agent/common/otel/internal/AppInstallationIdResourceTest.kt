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

package com.splunk.rum.agent.common.otel.internal

import com.splunk.rum.agent.common.storage.IAgentStorage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.resources.Resource
import org.junit.Assert.assertEquals
import org.junit.Test

class AppInstallationIdResourceTest {

    @Test
    fun `remove keeps other resource attributes and schema url`() {
        val attributes = Attributes.of(SERVICE_NAME_KEY, "app", APP_VERSION_KEY, "1.0")
        val resource = Resource.create(
            attributes.toBuilder().put(APP_INSTALLATION_ID_KEY, IAgentStorage.PENDING_APP_INSTALLATION_ID).build(),
            SCHEMA_URL
        )

        assertEquals(Resource.create(attributes, SCHEMA_URL), AppInstallationIdResource.remove(resource))
    }

    private companion object {
        const val SCHEMA_URL = "https://opentelemetry.io/schemas/1.0.0"
        val SERVICE_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("service.name")
        val APP_VERSION_KEY: AttributeKey<String> = AttributeKey.stringKey("app.version")
        val APP_INSTALLATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("app.installation.id")
    }
}
