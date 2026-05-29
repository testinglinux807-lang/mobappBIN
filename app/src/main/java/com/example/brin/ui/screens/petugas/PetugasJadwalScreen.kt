package com.example.brin.ui.screens.petugas

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.TaskStatus
import com.example.brin.ui.screens.shared.taskStatusColor
import com.example.brin.ui.screens.shared.taskStatusLabel
import com.example.brin.ui.theme.*

private data class JadwalPetugas(val ruteId: String, val zona: String, val binCount: Int, val jamMulai: String, val jamSelesai: String, val status: TaskStatus)

private val jadwalHariIni = listOf(
    JadwalPetugas("TRK-01", "Zona A", 6, "08.00", "10.00", TaskStatus.PENDING),
    JadwalPetugas("TRK-03", "Zona A", 4, "13.00", "14.30", TaskStatus.DONE),
)

@Composable
fun PetugasJadwalScreen() {
    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 24.dp)
        ) {
            Text("Jadwal Saya", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Jumat, 22 Mei 2026", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                JadwalStat("2", "Total Tugas")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                JadwalStat("1", "Belum Mulai")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                JadwalStat("1", "Selesai")
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Jadwal Hari Ini", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

            jadwalHariIni.forEach { jadwal ->
                JadwalCard(jadwal)
            }

            Spacer(Modifier.height(8.dp))
            Text("Informasi", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRow(Icons.Default.Person, "Nama", "Andrian")
                    HorizontalDivider(color = DividerColor)
                    InfoRow(Icons.Default.LocationOn, "Zona", "Zona A")
                    HorizontalDivider(color = DividerColor)
                    InfoRow(Icons.Default.LocalShipping, "Truk", "TRK-01 & TRK-03")
                }
            }
        }
    }
}

@Composable
private fun JadwalStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun JadwalCard(jadwal: JadwalPetugas) {
    val statusColor = taskStatusColor(jadwal.status)
    val statusLabel = taskStatusLabel(jadwal.status)

    Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(GreenSurface), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(jadwal.ruteId, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${jadwal.zona} · ${jadwal.binCount} bin", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoChip(Icons.Default.Schedule, "${jadwal.jamMulai} – ${jadwal.jamSelesai}")
                InfoChip(Icons.Default.DeleteOutline, "${jadwal.binCount} bin")
                InfoChip(Icons.Default.LocationOn, jadwal.zona)
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextHint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextHint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
