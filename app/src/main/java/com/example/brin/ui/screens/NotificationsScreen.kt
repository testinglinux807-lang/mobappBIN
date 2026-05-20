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
import androidx.compose.ui.graphics.vector.ImageVector
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
        Surface(color = CardBg) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Notifikasi", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (unreadCount > 0) {
                        Surface(shape = RoundedCornerShape(12.dp), color = StatusCritical) {
                            Text(
                                "$unreadCount baru",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color.White,
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier
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
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = label,
                                fontSize   = 12.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (isActive) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(top = 60.dp),
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
    val iconColor: Color
    val bgColor: Color
    val icon: ImageVector
    when (item.type) {
        BinStatus.CRITICAL    -> { iconColor = StatusCritical; bgColor = StatusCriticalBg; icon = Icons.Default.Warning }
        BinStatus.NEED_PICKUP -> { iconColor = StatusWarning;  bgColor = StatusWarningBg;  icon = Icons.Default.Info }
        BinStatus.NORMAL      -> { iconColor = StatusNormal;   bgColor = StatusNormalBg;   icon = Icons.Default.CheckCircle }
    }

    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = CardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    if (!item.isRead) {
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusCritical))
                    }
                }
                Text(item.binId, fontSize = 11.sp, color = iconColor, fontWeight = FontWeight.Medium)
                Text(item.message, fontSize = 12.sp, color = TextSecondary)
            }
            Text(item.time, fontSize = 10.sp, color = TextHint)
        }
    }
}
