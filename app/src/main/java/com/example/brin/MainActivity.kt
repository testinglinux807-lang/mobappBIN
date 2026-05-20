package com.example.brin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.brin.ui.components.AppBottomNavBar
import com.example.brin.ui.navigation.AppNavHost
import com.example.brin.ui.navigation.bottomNavRoutes
import com.example.brin.ui.theme.BRINTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BRINTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val showBottomBar = currentRoute in bottomNavRoutes

                Scaffold(
                    modifier    = Modifier.fillMaxSize(),
                    bottomBar   = {
                        if (showBottomBar) AppBottomNavBar(navController)
                    }
                ) { _ ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(navController)
                    }
                }
            }
        }
    }
}
