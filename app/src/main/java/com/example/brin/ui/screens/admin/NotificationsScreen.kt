package com.example.brin.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.brin.data.BinData
import com.example.brin.data.BinStatus
import com.example.brin.data.NotificationItem
import com.example.brin.data.api.ApiPickup
import com.example.brin.data.repository.AlertRepository
import com.example.brin.data.repository.BinRepository
import com.example.brin.data.repository.PickupRepository
import com.example.brin.data.websocket.WebSocketManager
import com.example.brin.data.websocket.WsEvent
import com.example.brin.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun NotificationsScreen(
    onBinClick: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    areaScopeId: String? = null,      // kalau di-set, notif difilter cuma bin di area ini (mode petugas)
    allowResolve: Boolean = true,     // false = sembunyikan tombol "Tandai selesai" (read-only)
    showPickupCards: Boolean = true   // false = sembunyikan kartu audit "Pickup Selesai" (khusus admin)
) {
    val scope         = rememberCoroutineScope()
    val context       = LocalContext.current
    var selectedTab   by remember { mutableIntStateOf(0) }
    var allAlerts     by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var pickups       by remember { mutableStateOf<List<ApiPickup>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(true) }
    var resolvingIds  by remember { mutableStateOf(setOf<String>()) }
    var confirmingPickupIds by remember { mutableStateOf(setOf<String>()) }
    var bins          by remember { mutableStateOf<List<BinData>>(emptyList()) }
    val tabs = listOf("Semua", "Kritis", "Pickup", "Info")

    LaunchedEffect(Unit) {
        BinRepository.getBins().onSuccess { bins = it }
        // Hanya alert yang belum di-resolve — yang sudah beres tidak ditampilkan.
        AlertRepository.getAlerts(resolved = false).onSuccess { allAlerts = it }
        PickupRepository.getPickups().onSuccess { list ->
            pickups = list
                .filter { (it.status == "SELESAI" || it.status == "MENUNGGU_SENSOR") && (areaScopeId == null || it.areaId == areaScopeId) }
                .sortedBy { if (it.status == "MENUNGGU_SENSOR") 0 else 1 }  // yang masih nunggu di atas
        }
        isLoading = false
    }

    // Buka titik GPS pickup di aplikasi peta
    val openMaps: (Double, Double) -> Unit = { lat, lng ->
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Lokasi Pickup)")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    // Konfirmasi manual pickup yang masih MENUNGGU_SENSOR (fallback sensor error).
    val onConfirmPickup: (ApiPickup) -> Unit = { pickup ->
        if (pickup.id !in confirmingPickupIds) {
            confirmingPickupIds = confirmingPickupIds + pickup.id
            scope.launch {
                PickupRepository.confirmPickup(pickup.id)
                    .onSuccess { updated -> pickups = pickups.map { if (it.id == updated.id) updated else it } }
                confirmingPickupIds = confirmingPickupIds - pickup.id
            }
        }
    }

    // Live alerts via WebSocket — patch state directly, no re-fetch
    LaunchedEffect(Unit) {
        WebSocketManager.events.collect { event ->
            when (event) {
                is WsEvent.AlertNew -> {
                    if (allAlerts.none { it.id == event.alertId }) {
                        val item = AlertRepository.liveAlertItem(
                            event.alertId, event.nodeId, event.binId,
                            event.type, event.message, event.createdAt
                        )
                        allAlerts = listOf(item) + allAlerts
                    }
                }
                is WsEvent.AlertResolved -> {
                    allAlerts = allAlerts.filter { it.id != event.alertId }   // alert beres → hilang dari notif
                }
                else -> Unit
            }
        }
    }

    val areaBinIds = remember(bins, areaScopeId) {
        if (areaScopeId == null) null else bins.filter { it.areaId == areaScopeId }.map { it.id }.toSet()
    }
    val nonNormalBinIds = remember(bins) { bins.filter { it.status != BinStatus.NORMAL }.map { it.id }.toSet() }
    // Hanya alert AKTIF: belum di-resolve, dan untuk alert PENUH binnya masih non-normal
    // (buang alert basi dari bin yang sudah kosong). Alert non-penuh (baterai/gas) tak di-guard volume.
    val scopedAlerts = remember(allAlerts, areaBinIds, nonNormalBinIds, areaScopeId) {
        allAlerts.filter { alert ->
            if (alert.isRead) return@filter false
            val isFull = alert.rawType == "FULL_VOLUME" || alert.rawType == "FULL_WEIGHT"
            if (isFull && alert.binRefId !in nonNormalBinIds) return@filter false
            areaScopeId == null || alert.binRefId in (areaBinIds ?: emptySet())
        }
    }
    val filtered = remember(selectedTab, scopedAlerts) {
        when (selectedTab) {
            1    -> scopedAlerts.filter { it.type == BinStatus.CRITICAL }
            2    -> scopedAlerts.filter { it.type == BinStatus.NEED_PICKUP }
            3    -> scopedAlerts.filter { it.type == BinStatus.NORMAL }
            else -> scopedAlerts
        }
    }
    val unreadCount = scopedAlerts.count { !it.isRead }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text("Notifikasi", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                // Chip "X baru" cuma relevan buat admin (yang bisa nge-resolve). Petugas read-only → disembunyikan.
                if (unreadCount > 0 && allowResolve) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFF5252)) {
                        Text("$unreadCount baru", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(CardBg).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isActive = selectedTab == index
                Surface(shape = RoundedCornerShape(20.dp), color = if (isActive) GreenPrimary else AppBackground) {
                    Text(
                        text = label, fontSize = 12.sp,
                        color = if (isActive) Color.White else TextSecondary,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.clickable { selectedTab = index }.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val showPickups = showPickupCards && (selectedTab == 0 || selectedTab == 2)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
            } else if (filtered.isEmpty() && (!showPickups || pickups.isEmpty())) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ada notifikasi", color = TextHint, fontSize = 14.sp)
                }
            } else {
                if (showPickups) {
                    pickups.forEach { pickup ->
                        PickupNotifCard(
                            pickup       = pickup,
                            onOpenMaps   = openMaps,
                            canConfirm   = allowResolve,
                            isConfirming = confirmingPickupIds.contains(pickup.id),
                            onConfirm    = { onConfirmPickup(pickup) },
                            onClick      = { onBinClick(pickup.binId) }
                        )
                    }
                }
                filtered.forEach { item ->
                    NotifCard(
                        item       = item,
                        isResolving = resolvingIds.contains(item.id),
                        canResolve = allowResolve,
                        onClick    = { onBinClick(item.binRefId) },
                        onResolve  = {
                            resolvingIds = resolvingIds + item.id
                            scope.launch {
                                AlertRepository.resolveAlert(item.id).onSuccess {
                                    allAlerts = allAlerts.filter { it.id != item.id }   // hilang dari list begitu beres
                                }
                                resolvingIds = resolvingIds - item.id
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotifCard(item: NotificationItem, isResolving: Boolean = false, canResolve: Boolean = true, onClick: () -> Unit = {}, onResolve: () -> Unit = {}) {
    val accentColor = when (item.type) {
        BinStatus.CRITICAL    -> StatusCritical
        BinStatus.NEED_PICKUP -> StatusWarning
        BinStatus.NORMAL      -> StatusNormal
    }
    val icon = when (item.type) {
        BinStatus.CRITICAL    -> Icons.Default.Warning
        BinStatus.NEED_PICKUP -> Icons.Default.LocalShipping
        BinStatus.NORMAL      -> Icons.Default.Notifications
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                fontSize = 14.sp,
                fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.SemiBold,
                color = if (item.isRead) TextSecondary else TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text("${item.binId} · ${item.message}", fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.time, fontSize = 11.sp, color = TextHint)
        }

        if (!item.isRead && canResolve) {
            Spacer(Modifier.width(8.dp))
            if (isResolving) {
                CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onResolve, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Tandai selesai", tint = GreenPrimary, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun PickupNotifCard(
    pickup: ApiPickup,
    onOpenMaps: (Double, Double) -> Unit,
    canConfirm: Boolean = false,
    isConfirming: Boolean = false,
    onConfirm: () -> Unit = {},
    onClick: () -> Unit
) {
    val isPending = pickup.status == "MENUNGGU_SENSOR"
    val node    = pickup.bin?.nodeId ?: pickup.binId
    val loc     = pickup.bin?.location ?: "-"
    val petugas = pickup.petugas?.name ?: "Petugas"
    val time    = (pickup.manualConfirmedAt ?: pickup.sensorConfirmedAt ?: pickup.completedAt).toClock()
    val title   = if (isPending) "Pickup · Menunggu Sensor" else "Pickup Selesai"
    val accent  = if (isPending) StatusWarning else StatusNormal

    val lat = pickup.completedLat
    val lng = pickup.completedLng
    val distM = if (lat != null && lng != null && pickup.bin != null)
        haversineMeters(lat, lng, pickup.bin.lat, pickup.bin.lng) else null
    val nearBin = distM != null && distM <= 50  // dalam 50 m dianggap valid

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("$node · $loc", fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("oleh $petugas · $time", fontSize = 11.sp, color = TextHint)
            }
        }

        if (distM != null) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (nearBin) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (nearBin) StatusNormal else StatusWarning,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (nearBin) "${formatDistance(distM)} dari bin · sesuai lokasi"
                           else "${formatDistance(distM)} dari bin · perlu dicek",
                    fontSize = 12.sp,
                    color = if (nearBin) StatusNormal else StatusWarning,
                    modifier = Modifier.weight(1f)
                )
                if (lat != null && lng != null) {
                    TextButton(onClick = { onOpenMaps(lat, lng) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Buka Maps", fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Tombol fallback konfirmasi manual (admin) — kalau sensor error / belum baca.
        if (isPending && canConfirm) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onConfirm,
                enabled = !isConfirming,
                colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape   = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                if (isConfirming) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tandai Selesai (Manual)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * atan2(sqrt(a), sqrt(1 - a))
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000) else "${meters.toInt()} m"

private fun String.toClock(): String = runCatching {
    java.time.format.DateTimeFormatter.ofPattern("dd MMM · HH:mm", java.util.Locale.forLanguageTag("id"))
        .withZone(java.time.ZoneId.systemDefault())
        .format(java.time.Instant.parse(this))
}.getOrDefault(this)
