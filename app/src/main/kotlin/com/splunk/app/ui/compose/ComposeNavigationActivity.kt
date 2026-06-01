@file:Suppress("ktlint:standard:function-naming")

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

package com.splunk.app.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.navigation.extension.navigation

class ComposeNavigationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ComposeNavigationApp(onBack = { finish() })
            }
        }
    }
}

@Composable
fun ComposeNavigationApp(onBack: () -> Unit = {}) {
    val navController = rememberNavController()

    SplunkRum.instance.navigation.registerNavController(navController)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Compose Navigation") })
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(navController, onBack)
            }
            composable("settings") {
                SettingsScreen(navController)
            }
            composable(
                route = "profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                ProfileScreen(
                    userId = backStackEntry.arguments?.getString("userId") ?: "unknown",
                    navController = navController
                )
            }
            composable(
                route = "search?query={query}",
                arguments = listOf(
                    navArgument("query") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                SearchScreen(
                    query = backStackEntry.arguments?.getString("query") ?: "",
                    navController = navController
                )
            }
            composable("nested_tabs") {
                NestedTabsScreen(navController)
            }
            dialog("confirm_dialog") {
                ConfirmDialog(navController)
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, onBack: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Home", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("settings") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Settings")
        }

        Button(
            onClick = { navController.navigate("profile/user_12345") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Profile (user_12345)")
        }

        Button(
            onClick = { navController.navigate("profile/user_67890") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Profile (user_67890)")
        }

        Button(
            onClick = { navController.navigate("search?query=opentelemetry") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search: opentelemetry")
        }

        Button(
            onClick = { navController.navigate("search") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search (no query)")
        }

        Button(
            onClick = { navController.navigate("confirm_dialog") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show Dialog")
        }

        Button(
            onClick = { navController.navigate("nested_tabs") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nested Tabs (own NavHost)")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Main Menu")
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}

@Composable
fun ProfileScreen(userId: String, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Profile", style = MaterialTheme.typography.h5)
        Text("User ID: $userId", style = MaterialTheme.typography.body1)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}

@Composable
fun SearchScreen(query: String, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Search", style = MaterialTheme.typography.h5)
        Text(
            text = if (query.isNotEmpty()) "Query: $query" else "No query provided",
            style = MaterialTheme.typography.body1
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}

@Composable
fun NestedTabsScreen(parentNavController: NavController) {
    val nestedNavController = rememberNavController()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Nested Tabs (separate NavController)",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(16.dp)
        )

        val currentRoute = nestedNavController.currentBackStackEntryAsState().value
            ?.destination?.route

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "tab_feed" to "Feed",
                "tab_trending" to "Trending",
                "tab_inbox" to "Inbox"
            )
            tabs.forEach { (route, label) ->
                val selected = currentRoute == route
                if (selected) {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            nestedNavController.navigate(route) {
                                popUpTo("tab_feed") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        NavHost(
            navController = nestedNavController,
            startDestination = "tab_feed",
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            composable("tab_feed") {
                TabContent("Feed", "Latest posts and updates")
            }
            composable("tab_trending") {
                TabContent("Trending", "Popular content right now")
            }
            composable("tab_inbox") {
                TabContent("Inbox", "Your messages and notifications")
            }
        }

        Button(
            onClick = { parentNavController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Back to Home")
        }
    }
}

@Composable
fun TabContent(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.body1)
    }
}

@Composable
fun ConfirmDialog(navController: NavController) {
    AlertDialog(
        onDismissRequest = { navController.popBackStack() },
        title = { Text("Confirm") },
        text = { Text("This is a dialog destination.") },
        confirmButton = {
            TextButton(onClick = { navController.popBackStack() }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = { navController.popBackStack() }) {
                Text("Cancel")
            }
        }
    )
}
