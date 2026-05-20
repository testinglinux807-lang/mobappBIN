package com.example.brin.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.brin.ui.screens.AnalyticsScreen
import com.example.brin.ui.screens.ScheduleScreen
import com.example.brin.ui.screens.DetailBinScreen
import com.example.brin.ui.screens.DetailRuteScreen
import com.example.brin.ui.screens.HomeScreen
import com.example.brin.ui.screens.LoginScreen
import com.example.brin.ui.screens.MapScreen
import com.example.brin.ui.screens.NotificationsScreen
import com.example.brin.ui.screens.SettingsScreen
import com.example.brin.ui.screens.SplashScreen
import com.example.brin.ui.screens.WelcomeScreen

sealed class Screen(val route: String) {
    object Splash        : Screen("splash")
    object Login         : Screen("login")
    object Welcome       : Screen("welcome/{name}") {
        fun go(name: String) = "welcome/$name"
    }
    object Home          : Screen("home")
    object Map           : Screen("map")
    object Notifications : Screen("notifications")
    object Analytics     : Screen("analytics")
    object Schedule      : Screen("schedule")
    object Settings      : Screen("settings")
    object DetailRute    : Screen("detail_rute/{routeId}") {
        fun go(routeId: String) = "detail_rute/$routeId"
    }
    object DetailBin     : Screen("detail_bin/{binId}") {
        fun go(binId: String) = "detail_bin/$binId"
    }
}

val bottomNavRoutes = listOf(
    Screen.Home.route,
    Screen.Map.route,
    Screen.Notifications.route,
    Screen.Schedule.route,
    Screen.Settings.route
)

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route,
        enterTransition  = { fadeIn(tween(300)) },
        exitTransition   = { fadeOut(tween(300)) }
    ) {

        // Splash → Login: splash scale down + fade out, login slide up + fade in
        composable(
            route       = Screen.Splash.route,
            exitTransition = {
                scaleOut(tween(500), targetScale = 1.1f) + fadeOut(tween(500))
            }
        ) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Login: slide up dari bawah saat masuk, slide up keluar saat login sukses
        composable(
            route            = Screen.Login.route,
            enterTransition  = {
                slideInVertically(tween(500)) { it / 3 } + fadeIn(tween(500))
            },
            exitTransition   = {
                scaleOut(tween(400), targetScale = 0.92f) + fadeOut(tween(400))
            }
        ) {
            LoginScreen(
                onLoginSuccess = { name ->
                    navController.navigate(Screen.Welcome.go(name)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route            = Screen.Welcome.route,
            arguments        = listOf(navArgument("name") { type = NavType.StringType }),
            enterTransition  = { fadeIn(tween(400)) },
            exitTransition   = { scaleOut(tween(500), targetScale = 1.08f) + fadeOut(tween(500)) }
        ) { back ->
            val name = back.arguments?.getString("name") ?: "Admin"
            WelcomeScreen(
                adminName  = name,
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // Bottom nav screens: fade in/out
        composable(
            route           = Screen.Home.route,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition  = { fadeOut(tween(200)) }
        ) {
            HomeScreen(
                onBinClick   = { navController.navigate(Screen.DetailBin.go(it)) },
                onRouteClick = { navController.navigate(Screen.DetailRute.go(it)) }
            )
        }

        composable(
            route           = Screen.Map.route,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition  = { fadeOut(tween(200)) }
        ) {
            MapScreen(onBinClick = { navController.navigate(Screen.DetailBin.go(it)) })
        }

        composable(
            route           = Screen.Notifications.route,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition  = { fadeOut(tween(200)) }
        ) { NotificationsScreen() }

        composable(
            route           = Screen.Analytics.route,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition  = { fadeOut(tween(200)) }
        ) { AnalyticsScreen() }

        composable(
            route           = Screen.Schedule.route,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition  = { fadeOut(tween(200)) }
        ) { ScheduleScreen() }

        composable(
            route           = Screen.Settings.route,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition  = { fadeOut(tween(200)) }
        ) {
            SettingsScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Detail screens: slide in dari kanan
        composable(
            route            = Screen.DetailRute.route,
            arguments        = listOf(navArgument("routeId") { type = NavType.StringType }),
            enterTransition  = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
            exitTransition   = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) { back ->
            DetailRuteScreen(
                routeId    = back.arguments?.getString("routeId") ?: "",
                onBack     = { navController.popBackStack() },
                onBinClick = { navController.navigate(Screen.DetailBin.go(it)) }
            )
        }

        composable(
            route            = Screen.DetailBin.route,
            arguments        = listOf(navArgument("binId") { type = NavType.StringType }),
            enterTransition  = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
            exitTransition   = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) { back ->
            DetailBinScreen(
                binId  = back.arguments?.getString("binId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
