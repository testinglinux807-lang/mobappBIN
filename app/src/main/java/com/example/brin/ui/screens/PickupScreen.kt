package com.example.brin.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.MockData
import com.example.brin.data.PickupRoute
import com.example.brin.data.TaskStatus
import com.example.brin.ui.theme.*

@Composable
fun PickupScreen(onRouteClick: (String) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tugas Saya", "Dalam Proses", "Selesai")

    val routesByTab = remember(selectedTab) {
        when (selectedTab) {
            0 -> MockData.routes.filter { it.status == TaskStatus.PENDING }
            1 -> MockData.routes.filter { it.status == TaskStatus.IN_PROGRESS }
            2 -> MockData.routes.filter { it.status == TaskStatus.DONE }
            else -> emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Header
        Surface(color = CardBg) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Pickup", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                // Tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SearchBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, label ->
                        val isActive = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) GreenPrimary else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = label,
                                fontSize   = 13.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (isActive) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Daftar Rute",
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
            modifier   = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            if (routesByTab.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada tugas", color = TextHint, fontSize = 14.sp)
                }
            } else {
                routesByTab.forEach { route ->
                    RouteCard(route = route, onClick = { onRouteClick(route.id) })
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun RouteCard(route: PickupRoute, onClick: () -> Unit) {
    val statusColor = taskStatusColor(route.status)
    val progress    = if (route.binCount > 0) route.completedCount.toFloat() / route.binCount else 0f

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = CardBg,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(route.id, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${route.zone} · ${route.binCount} bin", fontSize = 13.sp, color = TextSecondary)
                    Text("Mulai ${route.startTime}", fontSize = 12.sp, color = TextHint)
                }
                StatusBadge(route.status)
            }

            Spacer(Modifier.height(10.dp))

            // Progress
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${route.completedCount}/${route.binCount}", fontSize = 12.sp, color = TextSecondary)
                Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = if (route.status == TaskStatus.DONE) StatusNormal else TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = if (route.status == TaskStatus.DONE) StatusNormal else GreenPrimary,
                trackColor = DividerColor
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onClick,
                colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape   = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text(
                    text       = if (route.status == TaskStatus.DONE) "Lihat Rute" else "Mulai Rute",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
            }
        }
    }
}
