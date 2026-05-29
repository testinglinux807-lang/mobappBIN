package com.example.brin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.brin.ui.navigation.AppNavHost
import com.example.brin.ui.theme.BRINTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BRINTheme {
                val navController = rememberNavController()
                AppNavHost(navController)
            }
        }
    }
}
