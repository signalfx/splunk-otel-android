package com.splunk.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.splunk.rum.SplunkRum

class JetpackComposeActivity : ComponentActivity() {
    private var lastRoute: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val currentBackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackEntry?.destination?.route

            LaunchedEffect(currentRoute) {
                if (currentRoute != null) {
                    lastRoute = currentRoute
                    SplunkRum.getInstance().experimentalSetScreenName(currentRoute)
                }
            }

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Column(
                    modifier = Modifier.padding(innerPadding)
                ) {
                    NavHost(navController = navController, startDestination = "view-a") {
                        composable("view-a") { ViewA(onNavigation = { path -> navController.navigate(path) }) }
                        composable("view-b") { ViewB(onNavigation = { path -> navController.navigate(path) }) }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // doubled to clear both the last view and the previous last view
        SplunkRum.getInstance().experimentalSetScreenName(null)
        SplunkRum.getInstance().experimentalSetScreenName(null)
    }

    override fun onResume() {
        super.onResume()
        if (lastRoute != null) {
            SplunkRum.getInstance().experimentalSetScreenName(lastRoute, "Resumed")
        }
    }
}

@Composable
fun ViewA(onNavigation: (String) -> Unit) {
    Column {
        Text("View A")
        Button(onClick = { onNavigation("view-b")}) {
            Text("Go to B")
        }
    }
}

@Composable
fun ViewB(onNavigation: (String) -> Unit) {
    Column {
        Text("View B")
        Button(onClick = { onNavigation("view-a")}) {
            Text("Go to A")
        }
    }
}
