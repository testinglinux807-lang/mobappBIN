package com.example.brin.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.brin.data.local.AppState
import com.example.brin.data.repository.AuthRepository
import com.example.brin.data.websocket.WebSocketManager
import com.example.brin.data.websocket.WsEvent
import com.example.brin.util.AlertNotifier
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.brin.ui.components.PetugasBottomNavBar
import com.example.brin.ui.screens.petugas.PetugasHomeScreen
import com.example.brin.ui.screens.petugas.PetugasPengaturanScreen
import com.example.brin.ui.screens.petugas.PetugasPickupScreen
import com.example.brin.ui.screens.petugas.PetugasProfileScreen
import com.example.brin.ui.screens.petugas.PetugasRiwayatScreen
import com.example.brin.ui.screens.petugas.PetugasScanScreen
import com.example.brin.ui.screens.admin.NotificationsScreen
import com.example.brin.ui.screens.shared.DetailBinScreen
import com.example.brin.ui.screens.shared.DetailRuteScreen
import com.example.brin.ui.screens.shared.PanduanScreen

sealed class PetugasScreen(val route: String) {
    object Home    : PetugasScreen("petugas_home")
    object Pickup  : PetugasScreen("petugas_pickup")
    object Scan    : PetugasScreen("petugas_scan")
    object Profile : PetugasScreen("petugas_profile")
    object Notifications : PetugasScreen("petugas_notifications")
    object DetailRute : PetugasScreen("petugas_detail_rute/{routeId}") {
        fun go(routeId: String) = "petugas_detail_rute/$routeId"
    }
    object DetailBin : PetugasScreen("petugas_detail_bin/{binId}") {
        fun go(binId: String) = "petugas_detail_bin/$binId"
    }
    object Panduan : PetugasScreen("petugas_panduan")
    object Riwayat : PetugasScreen("petugas_riwayat")
    object Pengaturan : PetugasScreen("petugas_pengaturan")
}

val petugasBottomNavRoutes = listOf(
    PetugasScreen.Home.route,
    PetugasScreen.Pickup.route,
    PetugasScreen.Scan.route,
    PetugasScreen.Notifications.route,
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

    // Notifikasi tray HP: siapkan channel + minta izin (Android 13+) sekali saat masuk.
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        AlertNotifier.ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Munculkan notifikasi tray saat ada ALERT_NEW dari WebSocket (difilter ke area petugas).
    LaunchedEffect(Unit) {
        WebSocketManager.events.collect { event ->
            if (event is WsEvent.AlertNew) {
                val areaId = AppState.currentUser?.areaId
                if (areaId == null || event.areaId == null || event.areaId == areaId) {
                    val node = event.nodeId.ifBlank { event.binId }
                    AlertNotifier.show(
                        context = context,
                        notifId = event.alertId.hashCode(),
                        title   = AlertNotifier.titleFor(event.type),
                        message = "$node · ${event.message}"
                    )
                }
            }
        }
    }

    Scaffold(
        modifier     = Modifier.fillMaxSize(),
        // Scaffold jangan ikut-ikutan add nav bar padding sendiri — biar bottomBar
        // (yang udah punya navigationBarsPadding) yang pegang. Kalau tidak, padding
        // dobel dan layar jadi kegedean.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { if (showBottom) PetugasBottomNavBar(navController) }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            NavHost(
                navController    = navController,
                startDestination = PetugasScreen.Home.route,
                enterTransition  = { fadeIn(tween(350)) },
                exitTransition   = { fadeOut(tween(200)) }
            ) {
                composable(PetugasScreen.Home.route) {
                    PetugasHomeScreen(
                        onBinClick   = { navController.navigate(PetugasScreen.DetailBin.go(it)) },
                        onNotifClick = {
                            // Notifikasi sekarang tab bottom-nav, jadi navigasi pakai opsi yang sama biar back stack konsisten
                            navController.navigate(PetugasScreen.Notifications.route) {
                                popUpTo(PetugasScreen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(PetugasScreen.Pickup.route) {
                    PetugasPickupScreen(onBinClick = { navController.navigate(PetugasScreen.DetailBin.go(it)) })
                }
                composable(PetugasScreen.Scan.route) {
                    PetugasScanScreen(onBinClick = { navController.navigate(PetugasScreen.DetailBin.go(it)) })
                }
                composable(PetugasScreen.Notifications.route) {
                    NotificationsScreen(
                        onBinClick      = { navController.navigate(PetugasScreen.DetailBin.go(it)) },
                        areaScopeId     = AppState.currentUser?.areaId,
                        allowResolve    = false,
                        showPickupCards = false
                    )
                }
                composable(PetugasScreen.Profile.route) {
                    PetugasProfileScreen(
                        onLogout = {
                            scope.launch { AuthRepository.logout(context) }
                            WebSocketManager.disconnect()
                            onLogout()
                        },
                        onHelp = { navController.navigate(PetugasScreen.Panduan.route) },
                        onHistory = { navController.navigate(PetugasScreen.Riwayat.route) },
                        onSettings = { navController.navigate(PetugasScreen.Pengaturan.route) }
                    )
                }
                composable(
                    route            = PetugasScreen.Panduan.route,
                    enterTransition  = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition   = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    PanduanScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route            = PetugasScreen.Riwayat.route,
                    enterTransition  = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition   = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    PetugasRiwayatScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route            = PetugasScreen.Pengaturan.route,
                    enterTransition  = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                    exitTransition   = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
                ) {
                    PetugasPengaturanScreen(onBack = { navController.popBackStack() })
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
