package com.example.brin.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.BinData
import com.example.brin.data.BinStatus
import com.example.brin.data.MockData
import com.example.brin.ui.theme.*

@Composable
fun HomeScreen(onBinClick: (String) -> Unit, onRouteClick: (String) -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        // Header
        Surface(color = CardBg, tonalElevation = 0.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text       = "Halo, Andrian! 👋",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Text(
                        text     = "Administrator · Sistem BRIN",
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )
                }
                Box {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifikasi",
                        tint     = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(StatusCritical)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("11", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null,
                tint = TextHint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cari bin / lokasi...", color = TextHint, fontSize = 14.sp)
        }

        Spacer(Modifier.height(20.dp))

        // Ringkasan Hari Ini
        Text(
            text       = "Ringkasan Hari Ini",
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
            modifier   = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))

        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard2x2("248", "Bin Aktif",       GreenPrimary,      Icons.Default.DeleteOutline, "+12 dari kemarin", true,  Modifier.weight(1f))
                StatCard2x2("37",  "Perlu Pickup",    StatusWarning,     Icons.Default.Warning,        "+5 dari kemarin",  true,  Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard2x2("8",   "Kritis",          StatusCritical,    Icons.Default.ErrorOutline,   "-2 dari kemarin",  false, Modifier.weight(1f))
                StatCard2x2("19",  "Pickup Hari Ini", Color(0xFF1976D2), Icons.Default.LocalShipping,  "+3 dari kemarin",  true,  Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Peringatan Kritis
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Peringatan Kritis", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lihat semua", fontSize = 13.sp, color = GreenPrimary)
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = GreenPrimary, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        val criticals = MockData.bins.filter { it.status != BinStatus.NORMAL }
        criticals.forEach { bin ->
            AlertCard(bin, onClick = { onBinClick(bin.id) })
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(20.dp))

        // Status Bin
        Text(
            text       = "Status Bin",
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
            modifier   = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            shape    = RoundedCornerShape(12.dp),
            color    = CardBg,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column {
                MockData.bins.forEachIndexed { index, bin ->
                    BinTableRow(bin = bin, onClick = { onBinClick(bin.id) })
                    if (index < MockData.bins.lastIndex)
                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Laporan Terbaru
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Laporan Terbaru", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lihat semua", fontSize = 13.sp, color = GreenPrimary)
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = GreenPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            shape    = RoundedCornerShape(12.dp),
            color    = CardBg,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column {
                laporanList.forEachIndexed { index, laporan ->
                    LaporanRow(laporan)
                    if (index < laporanList.lastIndex)
                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard2x2(
    value: String,
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trend: String,
    trendUp: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = CardBg,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Icon background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (trendUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint     = if (trendUp) StatusNormal else StatusCritical,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(trend, fontSize = 11.sp, color = if (trendUp) StatusNormal else StatusCritical)
            }
        }
    }
}

@Composable
private fun AlertCard(bin: BinData, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = CardBg,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (iconColor, bgColor) = when (bin.status) {
                BinStatus.CRITICAL    -> StatusCritical to StatusCriticalBg
                BinStatus.NEED_PICKUP -> StatusWarning  to StatusWarningBg
                else                  -> StatusNormal   to StatusNormalBg
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (bin.status == BinStatus.CRITICAL) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(bin.id, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(" · ${bin.location}", fontSize = 13.sp, color = TextSecondary)
                }
                Text(
                    text = when (bin.status) {
                        BinStatus.CRITICAL    -> "Kapasitas ${bin.capacity}% · ${bin.alertText}"
                        BinStatus.NEED_PICKUP -> "Kapasitas ${bin.capacity}%"
                        else                  -> "Pickup selesai · ${bin.capacity}%"
                    },
                    fontSize = 12.sp,
                    color    = if (bin.status == BinStatus.CRITICAL) StatusCritical else StatusWarning
                )
            }
            Text(bin.lastUpdate.replace(" yang lalu",""), fontSize = 11.sp, color = TextHint)
        }
    }
}

@Composable
private fun BinTableRow(bin: BinData, onClick: () -> Unit) {
    val statusCol = statusColor(bin.status)
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.8f)) {
            Text(bin.id, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(bin.location, fontSize = 11.sp, color = TextSecondary)
        }
        Column(modifier = Modifier.weight(2f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress   = { bin.capacity / 100f },
                    modifier   = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color      = statusCol,
                    trackColor = DividerColor
                )
                Spacer(Modifier.width(6.dp))
                Text("${bin.capacity}%", fontSize = 11.sp, color = statusCol, fontWeight = FontWeight.SemiBold)
            }
            Text("${bin.temperature}°C", fontSize = 10.sp, color = TextHint)
        }
        Spacer(Modifier.width(8.dp))
        BinStatusBadge(bin.status)
    }
}

private data class LaporanItem(
    val judul: String,
    val deskripsi: String,
    val waktu: String,
    val tipe: LaporanTipe
)

private enum class LaporanTipe { SELESAI, KRITIS, PICKUP, INFO }

private val laporanList = listOf(
    LaporanItem("TRK-03 Selesai",        "Zona C — 6 bin berhasil dikosongkan",     "07 mnt lalu",  LaporanTipe.SELESAI),
    LaporanItem("BIN-042 Kritis",        "Kapasitas 96% di Darmo Kali",             "12 mnt lalu",  LaporanTipe.KRITIS),
    LaporanItem("Pickup TRK-02 Dimulai", "Zona B — 4 bin sedang dalam proses",      "28 mnt lalu",  LaporanTipe.PICKUP),
    LaporanItem("BIN-089 Terdeteksi",    "Kapasitas 78% — perlu segera dijadwal",   "45 mnt lalu",  LaporanTipe.INFO),
    LaporanItem("TRK-01 Dijadwalkan",    "Zona A — 6 bin, mulai 08.00",             "1 jam lalu",   LaporanTipe.PICKUP),
)

@Composable
private fun LaporanRow(item: LaporanItem) {
    val (iconColor, bgColor, icon) = when (item.tipe) {
        LaporanTipe.SELESAI -> Triple(StatusNormal,   StatusNormalBg,   Icons.Default.CheckCircle)
        LaporanTipe.KRITIS  -> Triple(StatusCritical, StatusCriticalBg, Icons.Default.Warning)
        LaporanTipe.PICKUP  -> Triple(GreenPrimary,   GreenSurface,     Icons.Default.LocalShipping)
        LaporanTipe.INFO    -> Triple(StatusWarning,  StatusWarningBg,  Icons.Default.Info)
    }
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.judul, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(item.deskripsi, fontSize = 12.sp, color = TextSecondary)
        }
        Text(item.waktu, fontSize = 11.sp, color = TextHint)
    }
}
