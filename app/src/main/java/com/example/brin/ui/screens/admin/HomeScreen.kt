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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.IntrinsicSize
import com.example.brin.data.BinData
import com.example.brin.data.BinStatus
import com.example.brin.data.MockData
import com.example.brin.data.local.AppState
import com.example.brin.data.repository.BinRepository
import com.example.brin.data.websocket.WebSocketManager
import com.example.brin.data.websocket.WsEvent
import com.example.brin.ui.screens.shared.BinStatusBadge
import com.example.brin.ui.screens.shared.statusColor
import com.example.brin.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(onBinClick: (String) -> Unit, onRouteClick: (String) -> Unit = {}, onAddBin: () -> Unit = {}) {
    var bins      by remember { mutableStateOf(MockData.bins) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        BinRepository.getBins().onSuccess { bins = it }
        isLoading = false
    }

    // Live updates via WebSocket
    LaunchedEffect(Unit) {
        WebSocketManager.events.collect { event ->
            when (event) {
                is WsEvent.BinUpdate -> bins = bins.map { bin ->
                    if (bin.id == event.binId) bin.copy(
                        capacity   = event.volume,
                        battery    = event.battery,
                        gas        = event.gas,
                        status     = when { event.volume >= 90 -> BinStatus.CRITICAL; event.volume >= 70 -> BinStatus.NEED_PICKUP; else -> BinStatus.NORMAL },
                        lastUpdate = "Baru saja"
                    ) else bin
                }
                else -> Unit
            }
        }
    }

    val criticals  = bins.filter { it.status != BinStatus.NORMAL }
    val today      = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id")))
    val greeting   = when (LocalDateTime.now().hour) { in 0..10 -> "Selamat pagi,"; in 11..14 -> "Selamat siang,"; in 15..17 -> "Selamat sore,"; else -> "Selamat malam," }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenDark)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 28.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(greeting, fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
                    Text(AppState.userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(2.dp))
                    Text(today, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                }
                Box(modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                    if (criticals.isNotEmpty()) Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5252)).align(Alignment.TopEnd))
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                HeaderStat("${bins.size}", "Aktif")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                HeaderStat("${bins.count { it.status == BinStatus.NEED_PICKUP }}", "Pickup")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                HeaderStat("${bins.count { it.status == BinStatus.CRITICAL }}", "Kritis")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                HeaderStat("${bins.count { it.battery <= 20 }}", "Baterai")
            }
        }

        Spacer(Modifier.height(20.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else {
            SectionHeader(title = "Peringatan Kritis", actionLabel = "Lihat semua")
            Spacer(Modifier.height(8.dp))
            if (criticals.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Semua bin dalam kondisi normal", fontSize = 13.sp, color = TextSecondary)
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    criticals.forEach { bin -> AlertRow(bin, onClick = { onBinClick(bin.id) }) }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(title = "Status Bin")
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    bins.forEachIndexed { index, bin ->
                        BinTableRow(bin = bin, onClick = { onBinClick(bin.id) })
                        if (index < bins.lastIndex) HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(title = "Laporan Terbaru", actionLabel = "Lihat semua")
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    laporanList.forEachIndexed { index, laporan ->
                        LaporanRow(laporan)
                        if (index < laporanList.lastIndex) HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    }
                }
            }
        }
    } // end Column
    FloatingActionButton(
        onClick          = onAddBin,
        containerColor   = GreenPrimary,
        contentColor     = Color.White,
        modifier         = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 100.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Tambah Bin")
    }
    } // end Box
}

@Composable
private fun HeaderStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        if (actionLabel != null) Text(actionLabel, fontSize = 13.sp, color = GreenMedium)
    }
}

@Composable
private fun AlertRow(bin: BinData, onClick: () -> Unit) {
    val (accentColor, labelText) = when (bin.status) {
        BinStatus.CRITICAL    -> StatusCritical to "Kritis"
        BinStatus.NEED_PICKUP -> StatusWarning  to "Perlu Pickup"
        else                  -> StatusNormal   to "Normal"
    }
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardBg).clickable(onClick = onClick)) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentColor))
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${bin.nodeId} · ${bin.location}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text("Kapasitas ${bin.capacity}%  ·  $labelText", fontSize = 12.sp, color = accentColor)
                }
                Text(bin.lastUpdate.replace(" yang lalu", ""), fontSize = 11.sp, color = TextHint)
            }
        }
    }
}

@Composable
private fun BinTableRow(bin: BinData, onClick: () -> Unit) {
    val statusCol = statusColor(bin.status)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.8f)) {
            Text(bin.nodeId, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(bin.location, fontSize = 11.sp, color = TextSecondary)
        }
        Column(modifier = Modifier.weight(2f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { bin.capacity / 100f },
                    modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = statusCol, trackColor = DividerColor
                )
                Spacer(Modifier.width(6.dp))
                Text("${bin.capacity}%", fontSize = 11.sp, color = statusCol, fontWeight = FontWeight.SemiBold)
            }
            Text("Gas: ${bin.gas.toInt()} ppm", fontSize = 10.sp, color = TextHint)
        }
        Spacer(Modifier.width(8.dp))
        BinStatusBadge(bin.status)
    }
}

private data class LaporanItem(val judul: String, val deskripsi: String, val waktu: String, val tipe: LaporanTipe)
private enum class LaporanTipe { SELESAI, KRITIS, PICKUP, INFO }

private val laporanList = listOf(
    LaporanItem("TRK-03 Selesai",        "Zona C — 6 bin berhasil dikosongkan",   "07 mnt lalu", LaporanTipe.SELESAI),
    LaporanItem("BIN-042 Kritis",        "Kapasitas 96% di Darmo Kali",           "12 mnt lalu", LaporanTipe.KRITIS),
    LaporanItem("Pickup TRK-02 Dimulai", "Zona B — 4 bin sedang dalam proses",    "28 mnt lalu", LaporanTipe.PICKUP),
    LaporanItem("BIN-089 Terdeteksi",    "Kapasitas 78% — perlu segera dijadwal", "45 mnt lalu", LaporanTipe.INFO),
    LaporanItem("TRK-01 Dijadwalkan",    "Zona A — 6 bin, mulai 08.00",           "1 jam lalu",  LaporanTipe.PICKUP),
)

@Composable
private fun LaporanRow(item: LaporanItem) {
    val dotColor = when (item.tipe) {
        LaporanTipe.SELESAI -> StatusNormal
        LaporanTipe.KRITIS  -> StatusCritical
        LaporanTipe.PICKUP  -> GreenPrimary
        LaporanTipe.INFO    -> StatusWarning
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.judul, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(item.deskripsi, fontSize = 12.sp, color = TextSecondary)
        }
        Text(item.waktu, fontSize = 11.sp, color = TextHint)
    }
}
