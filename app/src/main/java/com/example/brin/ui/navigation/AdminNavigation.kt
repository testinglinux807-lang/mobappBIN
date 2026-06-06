package com.example.brin.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.brin.data.repository.AuthRepository
import com.example.brin.data.websocket.WebSocketManager
import com.example.brin.ui.components.AdminBottomNavBar
import com.example.brin.ui.screens.admin.AnalyticsScreen
import kotlinx.coroutines.launch
import com.example.brin.ui.screens.admin.HomeScreen
import com.example.brin.ui.screens.admin.MapScreen
import com.example.brin.ui.screens.admin.NotificationsScreen
import com.example.brin.ui.screens.admin.SettingsScreen
import com.example.brin.ui.screens.admin.AreasScreen
import com.example.brin.ui.screens.admin.CreateEditBinScreen
import com.example.brin.ui.screens.admin.CreateEditUserScreen
import com.example.brin.ui.screens.admin.UsersScreen
import com.example.brin.ui.screens.shared.DetailBinScreen
import com.example.brin.ui.screens.shared.DetailRuteScreen

sealed class AdminScreen(val route: String) {
    object Home          : AdminScreen("admin_home")
    object Map           : AdminScreen("admin_map")
    object Notifications : AdminScreen("admin_notifications")
    object Analytics     : AdminScreen("admin_analytics")
    object Settings      : AdminScreen("admin_settings")
    object DetailBin     : AdminScreen("admin_detail_bin/{binId}") {
        fun go(binId: String) = "admin_detail_bin/$binId"
    }
    object DetailRute    : AdminScreen("admin_detail_rute/{routeId}") {
        fun go(routeId: String) = "admin_detail_rute/$routeId"
    }
    object CreateBin     : AdminScreen("admin_create_bin")
    object EditBin       : AdminScreen("admin_edit_bin/{binId}") {
        fun go(binId: String) = "admin_edit_bin/$binId"
    }
    object Users         : AdminScreen("admin_users")
    object CreateUser    : AdminScreen("admin_create_user")
    object EditUser      : AdminScreen("admin_edit_user/{userId}") {
        fun go(userId: String) = "admin_edit_user/$userId"
    }
    object Areas         : AdminScreen("admin_areas")
}

val adminBottomNavRoutes = listOf(
    AdminScreen.Home.route,
    AdminScreen.Map.route,
    AdminScreen.Notifications.route,
    AdminScreen.Settings.route,
)

@Composable
fun AdminNavHost(onLogout: () -> Unit) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStack     by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route
    val showBottom    = currentRoute in adminBottomNavRoutes

    LaunchedEffect(Unit) { WebSocketManager.connect() }
    DisposableEffect(Unit) { onDispose { WebSocketManager.disconnect() } }

    Scaffold(
        modifier  = Modifier.fillMaxSize(),
        bottomBar = { if (showBottom) AdminBottomNavBar(navController) }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController    = navController,
                startDestination = AdminScreen.Home.route,
                enterTransition  = { fadeIn(tween(350)) },
                exitTransition   = { fadeOut(tween(200)) }
            ) {
                composable(AdminScreen.Home.route) {
                    HomeScreen(
                        onBinClick   = { navController.navigate(AdminScreen.DetailBin.go(it)) },
                        onRouteClick = { navController.navigate(AdminScreen.DetailRute.go(it)) },
                        onAddBin     = { navController.navigate(AdminScreen.CreateBin.route) }
                    )
                }
                composable(AdminScreen.Map.route) {
                    MapScreen(onBinClick = { navController.navigate(AdminScreen.DetailBin.go(it)) })
                }
                composable(AdminScreen.Notifications.route) {
                    NotificationsScreen(onBinClick = { navController.navigate(AdminScreen.DetailBin.go(it)) })
                }
                composable(AdminScreen.Analytics.route)     { AnalyticsScreen() }
                composable(AdminScreen.Settings.route) {
                    SettingsScreen(
                        onLogout    = { scope.launch { AuthRepository.logout(context) }; WebSocketManager.disconnect(); onLogout() },
                        onUsers     = { navController.navigate(AdminScreen.Users.route) },
                        onAreas     = { navController.navigate(AdminScreen.Areas.route) }
                    )
                }
                composable(
                    route             = AdminScreen.Users.route,
                    enterTransition   = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition    = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    UsersScreen(
                        onBack = { navController.popBackStack() },
                        onAdd  = { navController.navigate(AdminScreen.CreateUser.route) },
                        onEdit = { navController.navigate(AdminScreen.EditUser.go(it)) }
                    )
                }
                composable(
                    route             = AdminScreen.CreateUser.route,
                    enterTransition   = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition    = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    CreateEditUserScreen(userId = null, onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
                }
                composable(
                    route             = AdminScreen.EditUser.route,
                    arguments         = listOf(navArgument("userId") { type = NavType.StringType }),
                    enterTransition   = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition    = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) { back ->
                    CreateEditUserScreen(userId = back.arguments?.getString("userId"), onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
                }
                composable(
                    route             = AdminScreen.Areas.route,
                    enterTransition   = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition    = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    AreasScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route             = AdminScreen.DetailBin.route,
                    arguments         = listOf(navArgument("binId") { type = NavType.StringType }),
                    enterTransition   = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition    = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) { back ->
                    val binId = back.arguments?.getString("binId") ?: ""
                    DetailBinScreen(
                        binId     = binId,
                        onBack    = { navController.popBackStack() },
                        onEdit    = { navController.navigate(AdminScreen.EditBin.go(binId)) },
                        onDeleted = { navController.popBackStack() }
                    )
                }
                composable(
                    route             = AdminScreen.CreateBin.route,
                    enterTransition   = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition    = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    CreateEditBinScreen(
                        binId   = null,
                        onBack  = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
                composable(
                    route             = AdminScreen.EditBin.route,
                    arguments         = listOf(navArgument("binId") { type = NavType.StringType }),
                    enterTransition   = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition    = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) { back ->
                    CreateEditBinScreen(
                        binId   = back.arguments?.getString("binId"),
                        onBack  = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
                composable(
                    route            = AdminScreen.DetailRute.route,
                    arguments        = listOf(navArgument("routeId") { type = NavType.StringType }),
                    enterTransition  = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition   = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) { back ->
                    DetailRuteScreen(
                        routeId    = back.arguments?.getString("routeId") ?: "",
                        onBack     = { navController.popBackStack() },
                        onBinClick = { navController.navigate(AdminScreen.DetailBin.go(it)) }
                    )
                }
            }
        }
    }
}
