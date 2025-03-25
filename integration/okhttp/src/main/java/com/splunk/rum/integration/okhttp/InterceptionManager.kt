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

package com.splunk.rum.integration.okhttp

import com.cisco.android.common.utils.MutableListObserver
import com.splunk.rum.integration.okhttp.interceptor.SplunkOkHttpInterceptor
import com.splunk.rum.integration.okhttp.listener.OkHttpConnectorListenerImpl
import okhttp3.OkHttpClient

internal class InterceptionManager(private val listener: OkHttpConnector.Listener) {

    private val connectors = mutableMapOf<Int, OkHttpConnector>()

    fun getOkHttpInterceptors(builder: OkHttpClient.Builder): MutableList<SplunkOkHttpInterceptor> {
        val builderHash = builder.hashCode()
        val interceptors = connectors[builderHash]?.interceptors

        return if (interceptors == null) {
            val observer = object : MutableListObserver.Observer<SplunkOkHttpInterceptor> {
                override fun onRemoved(element: SplunkOkHttpInterceptor) {
                    val connector = connectors[builderHash] ?: return
                    if (connector.interceptors.isEmpty()) {
                        connector.unregister()
                        connectors.remove(builderHash)
                    }
                }
            }

            val connector = OkHttpConnector(builder, listener, observer)
            connector.register()

            connectors[builderHash] = connector
            connector.interceptors
        } else
            interceptors
    }

    companion object {
        val instance by lazy { InterceptionManager(OkHttpConnectorListenerImpl()) }
    }
}
