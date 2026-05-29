package com.example.brin.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.brin.data.UserRole
import com.example.brin.ui.screens.auth.LoginScreen
import com.example.brin.ui.screens.auth.SplashScreen
import com.example.brin.ui.screens.auth.WelcomeScreen

sealed class Screen(val route: String) {
    object Splash       : Screen("splash")
    object Login        : Screen("login")
    object Welcome      : Screen("welcome/{name}/{role}") {
        fun go(name: String, role: UserRole) = "welcome/$name/${role.name}"
    }
    object AdminGraph   : Screen("admin_graph")
    object PetugasGraph : Screen("petugas_graph")
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route,
        enterTransition  = { fadeIn(tween(300)) },
        exitTransition   = { fadeOut(tween(300)) }
    ) {
        composable(
            route          = Screen.Splash.route,
            exitTransition = { scaleOut(tween(500), targetScale = 1.1f) + fadeOut(tween(500)) }
        ) {
            SplashScreen(
                onAutoLogin = { name, role ->
                    val dest = if (role == UserRole.ADMIN) Screen.AdminGraph.route else Screen.PetugasGraph.route
                    navController.navigate(dest) { popUpTo(Screen.Splash.route) { inclusive = true } }
                },
                onNeedLogin = {
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                }
            )
        }

        composable(
            route           = Screen.Login.route,
            enterTransition = { slideInVertically(tween(500)) { it / 3 } + fadeIn(tween(500)) },
            exitTransition  = { scaleOut(tween(400), targetScale = 0.92f) + fadeOut(tween(400)) }
        ) {
            LoginScreen(
                onLoginSuccess = { name, role ->
                    navController.navigate(Screen.Welcome.go(name, role)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = Screen.Welcome.route,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType }
            ),
            enterTransition = { fadeIn(tween(400)) },
            exitTransition  = { scaleOut(tween(500), targetScale = 1.08f) + fadeOut(tween(500)) }
        ) { back ->
            val name = back.arguments?.getString("name") ?: "Pengguna"
            val role = runCatching { UserRole.valueOf(back.arguments?.getString("role") ?: "ADMIN") }.getOrDefault(UserRole.ADMIN)
            WelcomeScreen(
                userName  = name,
                userRole  = role,
                onFinished = {
                    val dest = if (role == UserRole.ADMIN) Screen.AdminGraph.route else Screen.PetugasGraph.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminGraph.route) {
            AdminNavHost(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PetugasGraph.route) {
            PetugasNavHost(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
