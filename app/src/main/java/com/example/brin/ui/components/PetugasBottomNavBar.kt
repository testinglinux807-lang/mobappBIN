package com.example.brin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.brin.data.local.AppState
import com.example.brin.data.local.NotifSeenStorage
import com.example.brin.data.repository.AlertRepository
import com.example.brin.data.repository.BinRepository
import com.example.brin.data.repository.PickupRepository
import com.example.brin.data.websocket.WebSocketManager
import com.example.brin.ui.navigation.PetugasScreen
import com.example.brin.ui.theme.*

private data class PetugasNavItem(val route: String, val icon: ImageVector, val label: String, val badge: Int = 0)

@Composable
fun PetugasBottomNavBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route

    // Badge tab: Pickup = bin belum normal, Notifikasi = alert belum dilihat. Di-refresh tiap ada event WebSocket.
    var needPickupCount by remember { mutableIntStateOf(0) }
    var unreadAlertIds  by remember { mutableStateOf<List<String>>(emptyList()) }
    suspend fun refreshBadge() {
        val bins = BinRepository.getBins().getOrDefault(emptyList())
        val pickups = PickupRepository.getPickups().getOrDefault(emptyList())
        val unread = AlertRepository.getAlerts(resolved = false).getOrDefault(emptyList())
        fun ms(s: String?): Long = s?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() } ?: Long.MIN_VALUE
        // Sama seperti layar Pickup: bin perlu pickup = punya alert penuh (FULL_*) belum
        // di-resolve & belum ada pickup yang dibuat setelah alert itu. Stabil terhadap
        // pembacaan sensor berulang, jadi badge ikut bersih setelah konfirmasi manual.
        val openFullAlertAt = unread
            .filter { it.rawType == "FULL_VOLUME" || it.rawType == "FULL_WEIGHT" }
            .groupBy { it.binRefId }
            .mapValues { (_, list) -> list.minOf { ms(it.createdAtRaw) } }
        needPickupCount = bins.count { bin ->
            val openAt = openFullAlertAt[bin.id]
            openAt != null && pickups.none { it.binId == bin.id && ms(it.completedAt) >= openAt }
        }
        val areaId = AppState.currentUser?.areaId
        unreadAlertIds = if (areaId == null) unread.map { it.id }
                         else bins.filter { it.areaId == areaId }.map { it.id }.toSet()
                             .let { ids -> unread.filter { a -> a.binRefId in ids }.map { it.id } }
    }
    LaunchedEffect(Unit) { refreshBadge() }
    LaunchedEffect(Unit) { WebSocketManager.events.collect { refreshBadge() } }

    // Badge Notifikasi = alert yang belum ditandai "dilihat". Saat tab Notifikasi aktif, tandai semua dilihat → badge hilang.
    // Disimpan di DataStore, jadi tetap hilang walau app ditutup & dibuka lagi.
    val context = LocalContext.current
    val notifStore = remember { NotifSeenStorage(context) }
    val seenIds by notifStore.seenIds.collectAsState(initial = emptySet())
    val unreadNotif = unreadAlertIds.count { it !in seenIds }
    LaunchedEffect(currentRoute, unreadAlertIds) {
        if (currentRoute == PetugasScreen.Notifications.route) notifStore.markSeen(unreadAlertIds)
    }

    val items = listOf(
        PetugasNavItem(PetugasScreen.Home.route,    Icons.Default.Home,          "Beranda"),
        PetugasNavItem(PetugasScreen.Pickup.route,  Icons.Default.LocalShipping, "Pickup", needPickupCount),
        PetugasNavItem(PetugasScreen.Scan.route,    Icons.Default.QrCodeScanner, "Scan QR"),
        PetugasNavItem(PetugasScreen.Notifications.route, Icons.Default.Notifications, "Notifikasi", unreadNotif),
        PetugasNavItem(PetugasScreen.Profile.route, Icons.Default.Person,        "Profil"),
    )

    val activeIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(32.dp), color = CardBg, shadowElevation = 20.dp, modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                val itemWidth    = maxWidth / items.size
                val indicatorOff by animateDpAsState(
                    targetValue   = itemWidth * activeIndex,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label         = "petugasNavIndicator"
                )
                Box(modifier = Modifier.offset(x = indicatorOff).width(itemWidth).height(44.dp).padding(horizontal = 4.dp).clip(RoundedCornerShape(22.dp)).background(GreenPrimary))
                Row(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        val isActive = activeIndex == index
                        Box(
                            modifier = Modifier.width(itemWidth).height(44.dp).clip(RoundedCornerShape(22.dp)).clickable(
                                interactionSource = remember { MutableInteractionSource() }, indication = null
                            ) { navController.navigate(item.route) { popUpTo(PetugasScreen.Home.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            contentAlignment = Alignment.Center
                        ) {
                            Box {
                                Icon(item.icon, contentDescription = item.label, tint = if (isActive) Color.White else TextHint, modifier = Modifier.size(22.dp))
                                if (item.badge > 0) {
                                    // Ring tipis warna background biar badge "ngangkat" dari ikon (gaya WA/Telegram)
                                    Box(
                                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-8).dp)
                                            .clip(CircleShape).background(CardBg).padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier.defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                                                .clip(CircleShape).background(Color(0xFFFF3B30))
                                                .padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                if (item.badge > 99) "99+" else "${item.badge}",
                                                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                                lineHeight = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
