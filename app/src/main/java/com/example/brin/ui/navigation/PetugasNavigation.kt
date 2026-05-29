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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.brin.data.repository.AuthRepository
import com.example.brin.data.websocket.WebSocketManager
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.brin.ui.components.PetugasBottomNavBar
import com.example.brin.ui.screens.petugas.PetugasHomeScreen
import com.example.brin.ui.screens.petugas.PetugasJadwalScreen
import com.example.brin.ui.screens.petugas.PetugasPickupScreen
import com.example.brin.ui.screens.petugas.PetugasProfileScreen
import com.example.brin.ui.screens.petugas.PetugasScanScreen
import com.example.brin.ui.screens.shared.DetailBinScreen
import com.example.brin.ui.screens.shared.DetailRuteScreen

sealed class PetugasScreen(val route: String) {
    object Home    : PetugasScreen("petugas_home")
    object Pickup  : PetugasScreen("petugas_pickup")
    object Scan    : PetugasScreen("petugas_scan")
    object Jadwal  : PetugasScreen("petugas_jadwal")
    object Profile : PetugasScreen("petugas_profile")
    object DetailRute : PetugasScreen("petugas_detail_rute/{routeId}") {
        fun go(routeId: String) = "petugas_detail_rute/$routeId"
    }
    object DetailBin : PetugasScreen("petugas_detail_bin/{binId}") {
        fun go(binId: String) = "petugas_detail_bin/$binId"
    }
}

val petugasBottomNavRoutes = listOf(
    PetugasScreen.Home.route,
    PetugasScreen.Pickup.route,
    PetugasScreen.Scan.route,
    PetugasScreen.Jadwal.route,
    PetugasScreen.Profile.route,
)

@Composable
fun PetugasNavHost(onLogout: () -> Unit) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStack     by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route
    val showBottom    = currentRoute in petugasBottomNavRoutes

    LaunchedEffect(Unit) { WebSocketManager.connect() }
    DisposableEffect(Unit) { onDispose { WebSocketManager.disconnect() } }

    Scaffold(
        modifier  = Modifier.fillMaxSize(),
        bottomBar = { if (showBottom) PetugasBottomNavBar(navController) }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController    = navController,
                startDestination = PetugasScreen.Home.route,
                enterTransition  = { fadeIn(tween(350)) },
                exitTransition   = { fadeOut(tween(200)) }
            ) {
                composable(PetugasScreen.Home.route) {
                    PetugasHomeScreen(onRouteClick = { navController.navigate(PetugasScreen.DetailRute.go(it)) })
                }
                composable(PetugasScreen.Pickup.route) {
                    PetugasPickupScreen(onRouteClick = { navController.navigate(PetugasScreen.DetailRute.go(it)) })
                }
                composable(PetugasScreen.Scan.route) {
                    PetugasScanScreen(onBinClick = { navController.navigate(PetugasScreen.DetailBin.go(it)) })
                }
                composable(PetugasScreen.Jadwal.route)  { PetugasJadwalScreen() }
                composable(PetugasScreen.Profile.route) {
                    PetugasProfileScreen(onLogout = {
                        scope.launch { AuthRepository.logout(context) }
                        WebSocketManager.disconnect()
                        onLogout()
                    })
                }
                composable(
                    route            = PetugasScreen.DetailRute.route,
                    arguments        = listOf(navArgument("routeId") { type = NavType.StringType }),
                    enterTransition  = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition   = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) { back ->
                    DetailRuteScreen(
                        routeId    = back.arguments?.getString("routeId") ?: "",
                        onBack     = { navController.popBackStack() },
                        onBinClick = { navController.navigate(PetugasScreen.DetailBin.go(it)) }
                    )
                }
                composable(
                    route            = PetugasScreen.DetailBin.route,
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
    }
}
