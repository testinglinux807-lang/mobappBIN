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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.BinStatus
import com.example.brin.data.MockData
import com.example.brin.data.PickupRoute
import com.example.brin.data.TaskStatus
import com.example.brin.data.local.AppState
import com.example.brin.data.repository.BinRepository
import com.example.brin.ui.screens.shared.taskStatusColor
import com.example.brin.ui.screens.shared.taskStatusLabel
import com.example.brin.ui.theme.*
import java.time.LocalDateTime

@Composable
fun PetugasHomeScreen(onRouteClick: (String) -> Unit = {}) {
    var bins by remember { mutableStateOf(MockData.bins) }
    LaunchedEffect(Unit) { BinRepository.getBins().onSuccess { bins = it } }

    val greeting = when (LocalDateTime.now().hour) { in 0..10 -> "Selamat pagi,"; in 11..14 -> "Selamat siang,"; else -> "Selamat sore," }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 28.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(greeting, fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
                    Text(AppState.userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(2.dp))
                    Text("Petugas Lapangan · ${AppState.areaId ?: "Zona A"}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                }
                Box(modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifikasi", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5252)).align(Alignment.TopEnd))
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(20.dp))

            val kritisCount  = bins.count { it.status == BinStatus.CRITICAL }
            val pickupCount  = bins.count { it.status == BinStatus.NEED_PICKUP }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                HeaderStat("${bins.size}", "Total")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                HeaderStat("$pickupCount", "Pickup")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                HeaderStat("$kritisCount", "Kritis")
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 80.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // Tugas aktif
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tugas Hari Ini", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Lihat semua", fontSize = 13.sp, color = GreenMedium)
            }
            Spacer(Modifier.height(8.dp))

            val activeRoutes = MockData.routes.filter { it.status != TaskStatus.DONE }.take(2)
            if (activeRoutes.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ada tugas aktif", color = TextHint, fontSize = 14.sp)
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activeRoutes.forEach { route -> RouteTaskCard(route, onClick = { onRouteClick(route.id) }) }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Info ringkas
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Info Bin", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoItem(Icons.Default.DeleteOutline, "Total Bin", "${bins.size} bin")
                    HorizontalDivider(color = DividerColor)
                    InfoItem(Icons.Default.Warning, "Bin Perlu Perhatian", "${bins.count { it.status != BinStatus.NORMAL }} bin")
                    HorizontalDivider(color = DividerColor)
                    InfoItem(Icons.Default.CheckCircle, "Selesai Hari Ini", "${MockData.routes.count { it.status == TaskStatus.DONE }} rute")
                }
            }
        }
    }
}

@Composable
private fun HeaderStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun RouteTaskCard(route: PickupRoute, onClick: () -> Unit) {
    val statusColor = taskStatusColor(route.status)
    val statusLabel = taskStatusLabel(route.status)
    val progress    = if (route.binCount > 0) route.completedCount.toFloat() / route.binCount else 0f

    Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(GreenSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(route.id, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${route.zone} · ${route.binCount} bin · Mulai ${route.startTime}", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${route.completedCount}/${route.binCount} bin selesai", fontSize = 12.sp, color = TextSecondary)
                Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), color = statusColor, trackColor = DividerColor)
        }
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
