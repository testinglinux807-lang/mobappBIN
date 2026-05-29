package com.example.brin.ui.screens.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.brin.data.MockData
import com.example.brin.ui.theme.*

@Composable
fun DetailRuteScreen(
    routeId: String,
    onBack: () -> Unit,
    onBinClick: (String) -> Unit
) {
    val route = MockData.getRouteById(routeId)

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Surface(color = CardBg) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(route?.id ?: routeId, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${route?.zone ?: ""} · ${route?.binCount ?: 0} bin", fontSize = 13.sp, color = TextSecondary)
                }
                route?.let { StatusBadge(it.status) }
            }
        }

        if (route == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Rute tidak ditemukan", color = TextHint)
            }
            return
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(route.binCount.toString(), "Total Bin", Icons.Default.DeleteOutline)
                    VerticalDivider(Modifier.height(40.dp))
                    StatItem(route.completedCount.toString(), "Selesai", Icons.Default.CheckCircle)
                    VerticalDivider(Modifier.height(40.dp))
                    val pct = if (route.binCount > 0) (route.completedCount * 100 / route.binCount) else 0
                    StatItem("$pct%", "Progress", Icons.Default.BarChart)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Daftar Bin", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            route.bins.forEachIndexed { index, bin ->
                BinListItem(number = index + 1, bin = bin, onClick = { onBinClick(bin.id) })
                if (index < route.bins.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = DividerColor)
            }
        }

        Surface(color = CardBg, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 64.dp).height(52.dp)
            ) {
                Text("Mulai Rute", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun BinListItem(number: Int, bin: BinData, onClick: () -> Unit) {
    val statusCol = statusColor(bin.status)
    Row(
        modifier = Modifier.fillMaxWidth().background(CardBg).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(SearchBg), contentAlignment = Alignment.Center) {
            Text("$number", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(bin.id, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("Kapasitas ${bin.capacity}%", fontSize = 12.sp, color = statusCol)
        }
        BinStatusBadge(bin.status)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}
