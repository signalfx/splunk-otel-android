/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.integration.okhttp.interceptor

import com.cisco.android.common.utils.extensions.forEachFast
import com.splunk.rum.integration.agent.api.network.SplunkNetworkRequest
import com.splunk.rum.integration.okhttp.model.Mask
import com.splunk.rum.integration.okhttp.model.SplunkChain
import java.net.URL

/**
 * Interceptor that masks request URL.
 *
 * @param masks List of masks to be applied to URL.
 */
class SplunkMaskUrlInterceptor(
    val masks: List<Mask>
) : SplunkOkHttpInterceptor {

    override fun onIntercept(original: SplunkChain, intercepted: SplunkNetworkRequest): SplunkNetworkRequest {
        var processedUrl = intercepted.url.toString()

        masks.forEachFast { mask ->
            processedUrl = processedUrl.replace(mask.regex, mask.replaceWith)
        }

        intercepted.url = URL(processedUrl)

        return intercepted
    }
}
