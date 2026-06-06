package com.example.brin.ui.screens.petugas

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.BinData
import com.example.brin.data.BinStatus
import com.example.brin.data.NotificationItem
import com.example.brin.data.api.ApiPickup
import com.example.brin.data.repository.AlertRepository
import com.example.brin.data.repository.BinRepository
import com.example.brin.data.repository.PickupRepository
import com.example.brin.ui.screens.shared.statusColor
import com.example.brin.ui.screens.shared.statusLabel
import com.example.brin.ui.theme.*
import com.example.brin.util.LocationHelper
import kotlinx.coroutines.launch

@Composable
fun PetugasPickupScreen(onBinClick: (String) -> Unit) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Perlu Pickup", "Dalam Proses", "Selesai")

    var bins            by remember { mutableStateOf<List<BinData>>(emptyList()) }
    var pickups         by remember { mutableStateOf<List<ApiPickup>>(emptyList()) }
    var alerts          by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading       by remember { mutableStateOf(true) }
    var processingBinId by remember { mutableStateOf<String?>(null) }
    var confirmingId    by remember { mutableStateOf<String?>(null) }
    var pendingPickup   by remember { mutableStateOf<BinData?>(null) }

    suspend fun reload() {
        BinRepository.getBins().onSuccess { bins = it }
        PickupRepository.getPickups().onSuccess { pickups = it }
        AlertRepository.getAlerts(resolved = false).onSuccess { alerts = it }
    }

    LaunchedEffect(Unit) {
        reload()
        isLoading = false
    }

    // Buka navigasi ke lokasi bin via Google Maps (fallback ke aplikasi peta lain).
    val openNavigation: (BinData) -> Unit = { bin ->
        val navUri    = Uri.parse("google.navigation:q=${bin.lat},${bin.lng}")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).setPackage("com.google.android.apps.maps")
        val launched  = runCatching { context.startActivity(navIntent); true }.getOrDefault(false)
        if (!launched) {
            val geoUri = Uri.parse("geo:${bin.lat},${bin.lng}?q=${bin.lat},${bin.lng}(${bin.nodeId})")
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri)) }
        }
    }

    // Ambil GPS petugas, catat pickup ke backend, lalu buka navigasi ke lokasi bin.
    // Diasumsikan izin lokasi sudah ada saat dipanggil.
    fun runPickup(bin: BinData) {
        // Guard double-submit: kalau sudah ada yang diproses, abaikan tap berikutnya.
        // Flag di-set sinkron (di luar coroutine) supaya tap beruntun langsung ke-block.
        if (processingBinId != null) return
        processingBinId = bin.id
        scope.launch {
            val loc = LocationHelper.getCurrentLocation(context)
            if (loc == null) {
                processingBinId = null
                snackbar.showSnackbar("Gagal mendapatkan lokasi GPS. Pastikan GPS aktif lalu coba lagi.")
                return@launch
            }
            PickupRepository.completePickup(bin.id, loc.lat, loc.lng)
                .onSuccess {
                    reload()
                    processingBinId = null
                    selectedTab = 1
                    openNavigation(bin)
                }
                .onFailure {
                    processingBinId = null
                    snackbar.showSnackbar(it.message ?: "Gagal mencatat pickup")
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        val target  = pendingPickup
        pendingPickup = null
        if (granted && target != null) runPickup(target)
        else if (!granted) scope.launch { snackbar.showSnackbar("Izin lokasi diperlukan untuk menyelesaikan pickup.") }
    }

    // Dipanggil dari tombol kartu: cek izin dulu, baru catat pickup + buka maps.
    val onPickup: (BinData) -> Unit = { bin ->
        if (LocationHelper.hasPermission(context)) {
            runPickup(bin)
        } else {
            pendingPickup = bin
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Konfirmasi manual pickup yang masih MENUNGGU_SENSOR (fallback kalau sensor error / tak terbaca).
    fun confirmManual(pickup: ApiPickup) {
        if (confirmingId != null) return
        confirmingId = pickup.id
        scope.launch {
            PickupRepository.confirmPickup(pickup.id)
                .onSuccess {
                    reload()
                    confirmingId = null
                    selectedTab = 2
                    snackbar.showSnackbar("Pickup ditandai selesai (manual).")
                }
                .onFailure {
                    confirmingId = null
                    snackbar.showSnackbar(it.message ?: "Gagal konfirmasi manual")
                }
        }
    }

    val inProgress = remember(pickups) { pickups.filter { it.status == "MENUNGGU_SENSOR" } }
    val done       = remember(pickups) { pickups.filter { it.status == "SELESAI" } }
    // "Perlu Pickup" digerakkan oleh ALERT, bukan volume mentah: bin perlu pickup kalau
    // punya alert penuh (FULL_*) yang belum di-resolve, DAN belum ada pickup yang dibuat
    // setelah alert itu. Alert dibuat sekali per siklus (di-dedup backend) jadi penanda
    // waktunya stabil — tidak goyah walau sensor terus mengirim pembacaan 99%.
    val openFullAlertAt = remember(alerts) {
        alerts.filter { it.rawType == "FULL_VOLUME" || it.rawType == "FULL_WEIGHT" }
            .groupBy { it.binRefId }
            .mapValues { (_, list) -> list.minOf { parseInstantMs(it.createdAtRaw) } }
    }
    val needPickup = remember(bins, pickups, openFullAlertAt) {
        bins.filter { bin ->
            val openAt = openFullAlertAt[bin.id] ?: return@filter false
            pickups.none { it.binId == bin.id && parseInstantMs(it.completedAt) >= openAt }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()
                    .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 16.dp)
            ) {
                Column {
                    Text("Pickup", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${needPickup.size} bin perlu pickup", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.12f)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, label ->
                        val isActive = selectedTab == index
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) Color.White else Color.Transparent)
                                .clickable { selectedTab = index }.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isActive) GreenDark else Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
            } else {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    when (selectedTab) {
                        0 -> {
                            if (needPickup.isEmpty()) EmptyState("Tidak ada bin yang perlu pickup")
                            else needPickup.forEach { bin ->
                                BinPickupCard(
                                    bin          = bin,
                                    isProcessing = processingBinId == bin.id,
                                    onDetail     = { onBinClick(bin.id) },
                                    onPickup     = { onPickup(bin) }
                                )
                            }
                        }
                        1 -> {
                            if (inProgress.isEmpty()) EmptyState("Tidak ada pickup dalam proses")
                            else inProgress.forEach { pickup ->
                                PickupCard(
                                    pickup       = pickup,
                                    isConfirming = confirmingId == pickup.id,
                                    onConfirm    = { confirmManual(pickup) }
                                )
                            }
                        }
                        2 -> {
                            if (done.isEmpty()) EmptyState("Belum ada pickup selesai")
                            else done.forEach { pickup -> PickupCard(pickup) }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
private fun BinPickupCard(bin: BinData, isProcessing: Boolean, onDetail: () -> Unit, onPickup: () -> Unit) {
    val color = statusColor(bin.status)
    val label = statusLabel(bin.status)
    Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth().clickable(onClick = onDetail)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(bin.nodeId, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(bin.location, fontSize = 13.sp, color = TextSecondary)
                    if (bin.area.isNotEmpty()) Text(bin.area, fontSize = 11.sp, color = TextHint)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kapasitas ${bin.capacity}%", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
                Text("Baterai ${bin.battery}%", fontSize = 12.sp, color = TextHint)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { bin.capacity / 100f },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = color, trackColor = DividerColor
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDetail,
                    shape   = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Detail", fontSize = 13.sp)
                }
                Button(
                    onClick = onPickup,
                    enabled = !isProcessing,
                    colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape   = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(5.dp))
                        Text("Memproses...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    } else {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Pickup Sekarang", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickupCard(pickup: ApiPickup, isConfirming: Boolean = false, onConfirm: (() -> Unit)? = null) {
    val isSelesai = pickup.status == "SELESAI"
    val statusColor = if (isSelesai) StatusNormal else StatusWarning
    val statusText  = if (isSelesai) "Selesai" else "Menunggu Sensor"

    Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pickup.bin?.nodeId ?: pickup.binId, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(pickup.bin?.location ?: "-", fontSize = 13.sp, color = TextSecondary)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(8.dp))
            if (pickup.completedAt.isNotEmpty()) {
                TimeRow(Icons.Default.LocalShipping, "Pickup", pickup.completedAt.toDateTime(), TextSecondary)
            }
            val confirmedAt = pickup.sensorConfirmedAt
            val manualAt    = pickup.manualConfirmedAt
            when {
                confirmedAt != null -> {
                    Spacer(Modifier.height(4.dp))
                    TimeRow(Icons.Default.CheckCircle, "Dikonfirmasi sensor", confirmedAt.toDateTime(), StatusNormal)
                }
                manualAt != null -> {
                    Spacer(Modifier.height(4.dp))
                    TimeRow(Icons.Default.HowToReg, "Dikonfirmasi manual", manualAt.toDateTime(), StatusNormal)
                }
                else -> {
                    Spacer(Modifier.height(4.dp))
                    TimeRow(Icons.Default.HourglassEmpty, "Menunggu konfirmasi sensor", null, StatusWarning)
                }
            }

            // Tombol fallback: tutup manual kalau sensor error / tak kunjung baca.
            if (!isSelesai && onConfirm != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onConfirm,
                    enabled = !isConfirming,
                    colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape   = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
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
}

@Composable
private fun TimeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (value != null) "$label · $value" else "$label...",
            fontSize = 11.sp,
            color = if (value != null) TextHint else color
        )
    }
}

/** ISO-8601 → epoch millis. null/invalid → Long.MIN_VALUE (dianggap "paling lama"). */
private fun parseInstantMs(s: String?): Long =
    s?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() } ?: Long.MIN_VALUE

/** ISO-8601 UTC (mis. "2026-06-01T04:57:05.420Z") → "01 Jun 2026 · 11:57" di zona waktu perangkat. */
private fun String.toDateTime(): String = runCatching {
    val instant = java.time.Instant.parse(this)
    java.time.format.DateTimeFormatter
        .ofPattern("dd MMM yyyy · HH:mm", java.util.Locale.forLanguageTag("id"))
        .withZone(java.time.ZoneId.systemDefault())
        .format(instant)
}.getOrDefault(this)

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TextHint, fontSize = 14.sp)
    }
}
