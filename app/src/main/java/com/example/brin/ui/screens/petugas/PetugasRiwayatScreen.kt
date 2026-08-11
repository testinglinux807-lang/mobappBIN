package com.example.brin.ui.screens.petugas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.api.ApiPickup
import com.example.brin.data.local.AppState
import com.example.brin.data.repository.PickupRepository
import com.example.brin.ui.theme.*

/** Riwayat pickup yang sudah selesai (SELESAI) milik petugas yang login. */
@Composable
fun PetugasRiwayatScreen(onBack: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var riwayat   by remember { mutableStateOf<List<ApiPickup>>(emptyList()) }

    LaunchedEffect(Unit) {
        val myId = AppState.currentUser?.id
        PickupRepository.getPickups().onSuccess { list ->
            riwayat = list
                .filter { it.status == "SELESAI" && (myId == null || it.petugasId == myId) }
                .sortedByDescending { parseInstantMs(it.completedAt) }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding().padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Text("Riwayat Pickup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text("${riwayat.size} pickup selesai", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 20.dp, top = 2.dp))
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else if (riwayat.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, contentDescription = null, tint = TextHint, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Belum ada riwayat pickup", color = TextHint, fontSize = 14.sp)
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                riwayat.forEach { RiwayatCard(it) }
            }
        }
    }
}

@Composable
private fun RiwayatCard(pickup: ApiPickup) {
    val confirmedAt = pickup.sensorConfirmedAt
    val manualAt    = pickup.manualConfirmedAt
    Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pickup.bin?.nodeId ?: pickup.binId, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(pickup.bin?.location ?: "-", fontSize = 13.sp, color = TextSecondary)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = StatusNormal.copy(alpha = 0.12f)) {
                    Text("Selesai", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusNormal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(8.dp))
            if (pickup.completedAt.isNotEmpty()) {
                RiwayatTimeRow(Icons.Default.LocalShipping, "Pickup", pickup.completedAt.toRiwayatDateTime(), TextSecondary)
            }
            when {
                confirmedAt != null -> {
                    Spacer(Modifier.height(4.dp))
                    RiwayatTimeRow(Icons.Default.CheckCircle, "Dikonfirmasi sensor", confirmedAt.toRiwayatDateTime(), StatusNormal)
                }
                manualAt != null -> {
                    Spacer(Modifier.height(4.dp))
                    RiwayatTimeRow(Icons.Default.HowToReg, "Dikonfirmasi manual", manualAt.toRiwayatDateTime(), StatusNormal)
                }
            }
        }
    }
}

@Composable
private fun RiwayatTimeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text("$label · $value", fontSize = 11.sp, color = TextHint)
    }
}

/** ISO-8601 → epoch millis. null/invalid → Long.MIN_VALUE. */
private fun parseInstantMs(s: String?): Long =
    s?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() } ?: Long.MIN_VALUE

/** ISO-8601 UTC → "01 Jun 2026 · 11:57" di zona waktu perangkat. */
private fun String.toRiwayatDateTime(): String = runCatching {
    val instant = java.time.Instant.parse(this)
    java.time.format.DateTimeFormatter
        .ofPattern("dd MMM yyyy · HH:mm", java.util.Locale.forLanguageTag("id"))
        .withZone(java.time.ZoneId.systemDefault())
        .format(instant)
}.getOrDefault(this)
