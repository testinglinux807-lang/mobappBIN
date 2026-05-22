package com.example.brin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.brin.ui.theme.*

@Composable
fun SettingsScreen(onLogout: () -> Unit = {}) {
    var notifEnabled  by remember { mutableStateOf(true) }
    var criticalAlert by remember { mutableStateOf(true) }
    var autoRefresh   by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize().background(AppBackground)
    ) {
        // ── Green accent header ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenDark)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar + info
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("A", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text("Andrian", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "andrianadiwahyono01@gmail.com",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(22.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(18.dp))

            // Stat row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingStat("248", "Total Bin")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.18f)))
                SettingStat("12", "Petugas")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.18f)))
                SettingStat("6", "Zona Aktif")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.18f)))
                SettingStat("99%", "Uptime")
            }
        }

        // ── Scrollable content ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            SectionLabel("Notifikasi")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column {
                    ToggleRow(Icons.Default.Notifications, "Notifikasi Push",  notifEnabled)  { notifEnabled  = it }
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    ToggleRow(Icons.Default.Warning,       "Alert Bin Kritis", criticalAlert) { criticalAlert = it }
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    ToggleRow(Icons.Default.Refresh,       "Auto Refresh Data",autoRefresh)   { autoRefresh   = it }
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionLabel("Manajemen Sistem")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column {
                    MenuRow(Icons.Default.People,         "Manajemen Petugas")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    MenuRow(Icons.Default.LocationOn,     "Manajemen Zona")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    MenuRow(Icons.Default.DeleteOutline,  "Manajemen Bin")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    MenuRow(Icons.Default.Download,       "Export Laporan")
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionLabel("Lainnya")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column {
                    MenuRow(Icons.AutoMirrored.Filled.Help, "Bantuan & Dukungan")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    MenuRow(Icons.Default.Info,              "Tentang Aplikasi")
                }
            }

            Spacer(Modifier.height(20.dp))

            // Logout
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onLogout)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = StatusCritical,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Keluar",
                        fontSize = 14.sp,
                        color = StatusCritical,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = TextHint,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun MenuRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GreenPrimary
            )
        )
    }
}
