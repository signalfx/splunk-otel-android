package com.splunk.app.ui.endpointconfiguration

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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.splunk.app.R
import com.splunk.app.databinding.FragmentEndpointConfigurationBinding
import com.splunk.app.ui.BaseFragment
import com.splunk.rum.integration.agent.api.EndpointConfiguration
import com.splunk.rum.integration.agent.api.SplunkRum
import java.net.URL

/**
 * A fragment for testing endpoint configuration APIs.
 */
class EndpointConfigurationFragment : BaseFragment<FragmentEndpointConfigurationBinding>() {

    override val titleRes: Int = R.string.endpoint_configuration_title

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentEndpointConfigurationBinding
        get() = FragmentEndpointConfigurationBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(viewBinding) {
            setRealmToken.setOnClickListener { setEndpointWithRealmToken() }
            setTracesOnly.setOnClickListener { setEndpointWithTracesOnly() }
            setTracesAndLogs.setOnClickListener { setEndpointWithTracesAndLogs() }
            clearEndpoint.setOnClickListener { clearEndpoint() }
            logEndpoints.setOnClickListener { logCurrentEndpoints() }
        }
    }

    /**
     * Sets endpoint configuration using realm and token.
     * Please replace the fields with the actual realm and token you want to test this API with
     */
    private fun setEndpointWithRealmToken() {
        Log.i(TAG, "Setting endpoint with realm and token")
        try {
            SplunkRum.instance.preferences.endpointConfiguration =
                EndpointConfiguration(realm = "us0", rumAccessToken = "abc123")
            showToast("Endpoint set with realm and token")
            logCurrentEndpoints()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting endpoint with realm/token", e)
            showToast("Error: ${e.message}")
        }
    }

    /**
     * Sets endpoint configuration with traces URL only.
     * Please replace the fields with the actual URL you want to test with
     */
    private fun setEndpointWithTracesOnly() {
        Log.i(TAG, "Setting endpoint with traces only")
        try {
            SplunkRum.instance.preferences.endpointConfiguration =
                EndpointConfiguration(URL("https://custom-traces.example.com/v1/traces"))
            showToast("Endpoint set with traces only")
            logCurrentEndpoints()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting endpoint with traces only", e)
            showToast("Error: ${e.message}")
        }
    }

    /**
     * Sets endpoint configuration with both traces and logs URLs.
     * Please replace the fields with the actual URLs you want to test with
     */
    private fun setEndpointWithTracesAndLogs() {
        Log.i(TAG, "Setting endpoint with traces and logs")
        try {
            SplunkRum.instance.preferences.endpointConfiguration =
                EndpointConfiguration(
                    URL("https://custom-traces.example.com/v1/traces"),
                    URL("https://custom-logs.example.com/v1/logs")
                )
            showToast("Endpoint set with traces and logs")
            logCurrentEndpoints()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting endpoint with traces and logs", e)
            showToast("Error: ${e.message}")
        }
    }

    /**
     * Clears the endpoint configuration.
     */
    private fun clearEndpoint() {
        Log.i(TAG, "Clearing endpoint")
        try {
            SplunkRum.instance.preferences.endpointConfiguration = null
            showToast("Endpoint cleared")
            logCurrentEndpoints()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing endpoint", e)
            showToast("Error: ${e.message}")
        }
    }

    /**
     * Logs the current endpoint configuration from both State and Preferences.
     */
    private fun logCurrentEndpoints() {
        Log.i(TAG, "=== CURRENT ENDPOINT CONFIGURATION ===")

        Log.i(TAG, "--- state.endpointConfiguration ---")
        val stateEndpoint = SplunkRum.instance.state.endpointConfiguration
        logEndpointDetails(stateEndpoint, "State")

        Log.i(TAG, "--- preferences.endpointConfiguration ---")
        val prefsEndpoint = SplunkRum.instance.preferences.endpointConfiguration
        logEndpointDetails(prefsEndpoint, "Preferences")

        Log.i(TAG, "======================================")
        showToast("Endpoints logged - refer to logcat")
    }

    private fun logEndpointDetails(endpoint: EndpointConfiguration?, source: String) {
        if (endpoint != null) {
            Log.i(TAG, "  [$source] traceEndpoint: ${endpoint.traceEndpoint}")
            Log.i(TAG, "  [$source] sessionReplayEndpoint: ${endpoint.sessionReplayEndpoint}")
            Log.i(TAG, "  [$source] realm: ${endpoint.realm}")
            Log.i(TAG, "  [$source] rumAccessToken: ${endpoint.rumAccessToken}")
        } else {
            Log.i(TAG, "  [$source] null")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "EndpointConfiguration"
    }
}
