package com.example.brin.ui.screens.admin

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.BinStatus
import com.example.brin.data.MockData
import com.example.brin.data.TaskStatus
import com.example.brin.data.repository.BinRepository
import com.example.brin.ui.theme.*

private val zonaLabels  = listOf("Zona A", "Zona B", "Zona C", "Zona D", "Zona E")
private val zonaPickup  = listOf(6f, 4f, 6f, 5f, 3f)
private val trendData   = listOf(12f, 18f, 15f, 22f, 19f, 24f, 28f)
private val trendLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

@Composable
fun AnalyticsScreen() {
    var selectedPeriod by remember { mutableIntStateOf(1) }
    val periods = listOf("Hari", "Minggu", "Bulan")

    var bins by remember { mutableStateOf(MockData.bins) }
    LaunchedEffect(Unit) { BinRepository.getBins().onSuccess { bins = it } }

    val totalBin    = bins.size
    val binKritis   = bins.count { it.status == BinStatus.CRITICAL }
    val binPickup   = bins.count { it.status == BinStatus.NEED_PICKUP }
    val ruteSelesai = MockData.routes.count { it.status == TaskStatus.DONE }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Surface(color = CardBg) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Analitik", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(SearchBg).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    periods.forEachIndexed { index, label ->
                        val isActive = selectedPeriod == index
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) GreenPrimary else Color.Transparent)
                                .clickable { selectedPeriod = index }.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isActive) Color.White else TextSecondary)
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticCard("$totalBin",    "Total Bin",    GreenPrimary,   Icons.Default.DeleteOutline, Modifier.weight(1f))
                AnalyticCard("$ruteSelesai", "Rute Selesai", StatusNormal,   Icons.Default.CheckCircle,   Modifier.weight(1f))
                AnalyticCard("$binKritis",   "Bin Kritis",   StatusCritical, Icons.Default.Warning,        Modifier.weight(1f))
                AnalyticCard("$binPickup",   "Perlu Pickup", StatusWarning,  Icons.Default.Info,           Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pickup per Zona", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Total pickup berdasarkan zona", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(16.dp))
                    ZonaBarChart(labels = zonaLabels, values = zonaPickup, modifier = Modifier.fillMaxWidth().height(140.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Tren Pickup", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("7 hari terakhir", fontSize = 12.sp, color = TextSecondary)
                        }
                        Text("28 pickup", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                    Spacer(Modifier.height(16.dp))
                    TrendLineChart(data = trendData, labels = trendLabels, color = GreenPrimary, modifier = Modifier.fillMaxWidth().height(100.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Performa Zona", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        Triple("Zona A", 92, GreenPrimary),
                        Triple("Zona B", 78, StatusNormal),
                        Triple("Zona C", 100, GreenPrimary),
                        Triple("Zona D", 65, StatusWarning),
                        Triple("Zona E", 55, StatusWarning),
                    ).forEach { (zona, pct, color) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(zona, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.width(56.dp))
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)),
                                color = color, trackColor = DividerColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("$pct%", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AnalyticCard(value: String, label: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 9.sp, color = TextSecondary, lineHeight = 12.sp, modifier = Modifier.wrapContentWidth())
        }
    }
}

@Composable
private fun ZonaBarChart(labels: List<String>, values: List<Float>, modifier: Modifier) {
    val maxVal = values.max()
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val barWidth = size.width / (values.size * 2f)
            val gap = barWidth
            values.forEachIndexed { i, v ->
                val barHeight = (v / maxVal) * size.height * 0.85f
                val x = i * (barWidth + gap) + gap / 2
                val y = size.height - barHeight
                drawRoundRect(color = GreenPrimary, topLeft = Offset(x, y), size = Size(barWidth, barHeight), cornerRadius = CornerRadius(6f, 6f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(text = label.replace("Zona ", "Z"), fontSize = 10.sp, color = TextHint, modifier = Modifier.weight(1f).wrapContentWidth())
            }
        }
    }
}

@Composable
private fun TrendLineChart(data: List<Float>, labels: List<String>, color: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (data.size < 2) return@Canvas
            val max = data.max(); val min = data.min(); val range = (max - min).coerceAtLeast(1f); val step = size.width / (data.size - 1)
            val path = Path()
            data.forEachIndexed { i, v ->
                val x = i * step; val y = size.height - ((v - min) / range) * size.height * 0.85f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            data.forEachIndexed { i, v ->
                val x = i * step; val y = size.height - ((v - min) / range) * size.height * 0.85f
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label -> Text(label, fontSize = 9.sp, color = TextHint, modifier = Modifier.weight(1f).wrapContentWidth()) }
        }
    }
}
