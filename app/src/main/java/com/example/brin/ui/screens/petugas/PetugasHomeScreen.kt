package com.example.brin.ui.screens.petugas

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.BinData
import com.example.brin.data.BinStatus
import com.example.brin.data.NotificationItem
import com.example.brin.data.api.ApiPickup
import com.example.brin.data.local.AppState
import com.example.brin.data.repository.AlertRepository
import com.example.brin.data.repository.BinRepository
import com.example.brin.data.repository.PickupRepository
import com.example.brin.ui.screens.shared.statusColor
import com.example.brin.ui.screens.shared.statusLabel
import com.example.brin.ui.theme.*
import java.time.LocalDateTime

@Composable
fun PetugasHomeScreen(onBinClick: (String) -> Unit = {}, onNotifClick: () -> Unit = {}) {
    var bins          by remember { mutableStateOf<List<BinData>>(emptyList()) }
    var pickups       by remember { mutableStateOf<List<ApiPickup>>(emptyList()) }
    var alerts        by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var pickupSelesai by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        BinRepository.getBins().onSuccess { bins = it }
        PickupRepository.getPickups().onSuccess {
            pickups = it
            pickupSelesai = it.count { p -> p.status == "SELESAI" }
        }
        AlertRepository.getAlerts(resolved = false).onSuccess { alerts = it }
    }

    val greeting = when (LocalDateTime.now().hour) {
        in 0..10  -> "Selamat pagi,"
        in 11..14 -> "Selamat siang,"
        else      -> "Selamat sore,"
    }

    // Konsisten dengan layar Pickup & badge: bin "perlu perhatian" = punya alert penuh
    // (FULL_*) belum di-resolve & belum ada pickup setelah alert itu. Jadi setelah pickup /
    // konfirmasi manual, bin langsung hilang dari sini walau volume sensor masih tinggi.
    val openFullAlertAt = alerts.filter { it.rawType == "FULL_VOLUME" || it.rawType == "FULL_WEIGHT" }
        .groupBy { it.binRefId }
        .mapValues { (_, list) -> list.minOf { parseInstantMs(it.createdAtRaw) } }
    val needAttention = bins.filter { bin ->
        val openAt = openFullAlertAt[bin.id] ?: return@filter false
        pickups.none { it.binId == bin.id && parseInstantMs(it.completedAt) >= openAt }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 28.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(greeting, fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
                    Text(AppState.userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(2.dp))
                    Text("Petugas Lapangan", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                }
                Box(modifier = Modifier.clip(CircleShape).clickable { onNotifClick() }.padding(top = 4.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifikasi", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                    if (needAttention.isNotEmpty()) Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5252)).align(Alignment.TopEnd))
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                HeaderStat("${bins.size}", "Total")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                HeaderStat("${needAttention.count { it.status == BinStatus.NEED_PICKUP }}", "Pickup")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                HeaderStat("${needAttention.count { it.status == BinStatus.CRITICAL }}", "Kritis")
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {
            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Bin Perlu Perhatian", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("${needAttention.size} bin", fontSize = 13.sp, color = GreenMedium)
            }
            Spacer(Modifier.height(8.dp))

            if (needAttention.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("Semua bin dalam kondisi normal", color = TextHint, fontSize = 14.sp)
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    needAttention.forEach { bin -> BinAttentionCard(bin, onClick = { onBinClick(bin.id) }) }
                }
            }

            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoItem(Icons.Default.DeleteOutline, "Total Bin", "${bins.size} bin")
                    HorizontalDivider(color = DividerColor)
                    InfoItem(Icons.Default.Warning, "Bin Perlu Perhatian", "${needAttention.size} bin")
                    HorizontalDivider(color = DividerColor)
                    InfoItem(Icons.Default.CheckCircle, "Pickup Selesai", "$pickupSelesai pickup")
                }
            }
        }
    }
}

@Composable
private fun BinAttentionCard(bin: BinData, onClick: () -> Unit) {
    val color = statusColor(bin.status)
    val label = statusLabel(bin.status)
    Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(color))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bin.nodeId, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("${bin.location}${if (bin.area.isNotEmpty()) " · ${bin.area}" else ""}", fontSize = 12.sp, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text("${bin.capacity}%", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** ISO-8601 → epoch millis. null/invalid → Long.MIN_VALUE (dianggap "paling lama"). */
private fun parseInstantMs(s: String?): Long =
    s?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() } ?: Long.MIN_VALUE

@Composable
private fun HeaderStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}
