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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.BinStatus
import com.example.brin.data.MockData
import com.example.brin.data.NotificationItem
import com.example.brin.ui.theme.*

@Composable
fun NotificationsScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Semua", "Kritis", "Pickup", "Info")

    val filtered = remember(selectedTab) {
        when (selectedTab) {
            1    -> MockData.notifications.filter { it.type == BinStatus.CRITICAL }
            2    -> MockData.notifications.filter { it.type == BinStatus.NEED_PICKUP }
            3    -> MockData.notifications.filter { it.type == BinStatus.NORMAL }
            else -> MockData.notifications
        }
    }
    val unreadCount = MockData.notifications.count { !it.isRead }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {

        // ── Green accent header ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenDark)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Notifikasi",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (unreadCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFF5252)
                    ) {
                        Text(
                            "$unreadCount baru",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // ── Tab filter ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isActive = selectedTab == index
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isActive) GreenPrimary else AppBackground
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (isActive) Color.White else TextSecondary,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable { selectedTab = index }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // ── Notif list ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada notifikasi", color = TextHint, fontSize = 14.sp)
                }
            } else {
                filtered.forEach { item -> NotifCard(item) }
            }
        }
    }
}

@Composable
private fun NotifCard(item: NotificationItem) {
    val accentColor = when (item.type) {
        BinStatus.CRITICAL    -> StatusCritical
        BinStatus.NEED_PICKUP -> StatusWarning
        BinStatus.NORMAL      -> StatusNormal
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBg)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left border hanya untuk yang belum dibaca
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (!item.isRead) accentColor else Color.Transparent)
            )
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        item.binId,
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(item.message, fontSize = 12.sp, color = TextSecondary)
                }
                Text(item.time, fontSize = 10.sp, color = TextHint)
            }
        }
    }
}
