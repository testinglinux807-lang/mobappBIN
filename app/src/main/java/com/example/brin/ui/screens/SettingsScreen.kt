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
        modifier             = Modifier.fillMaxSize().background(AppBackground),
        horizontalAlignment  = Alignment.CenterHorizontally
    ) {
        Surface(color = CardBg, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Pengaturan",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Admin profile card
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier          = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(GreenSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null,
                            tint = GreenPrimary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Andrian", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Administrator · Sistem BRIN", fontSize = 13.sp, color = TextSecondary)
                        Text("andrianadiwahyono01@gmail.com", fontSize = 11.sp, color = TextHint)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Stats row
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AdminStatCol("248",  "Total Bin")
                    VerticalDivider(Modifier.height(36.dp))
                    AdminStatCol("12",   "Petugas")
                    VerticalDivider(Modifier.height(36.dp))
                    AdminStatCol("6",    "Zona Aktif")
                    VerticalDivider(Modifier.height(36.dp))
                    AdminStatCol("99%",  "Uptime")
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionLabel("Notifikasi")
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    ToggleRow(Icons.Default.Notifications, "Notifikasi Push", notifEnabled) { notifEnabled = it }
                    HorizontalDivider(color = DividerColor)
                    ToggleRow(Icons.Default.Warning, "Alert Bin Kritis", criticalAlert) { criticalAlert = it }
                    HorizontalDivider(color = DividerColor)
                    ToggleRow(Icons.Default.Refresh, "Auto Refresh Data", autoRefresh) { autoRefresh = it }
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionLabel("Manajemen Sistem")
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    MenuRow(Icons.Default.People,      "Manajemen Petugas")
                    HorizontalDivider(color = DividerColor)
                    MenuRow(Icons.Default.LocationOn,  "Manajemen Zona")
                    HorizontalDivider(color = DividerColor)
                    MenuRow(Icons.Default.DeleteOutline,"Manajemen Bin")
                    HorizontalDivider(color = DividerColor)
                    MenuRow(Icons.Default.Download,    "Export Laporan")
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionLabel("Lainnya")
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    MenuRow(Icons.AutoMirrored.Filled.Help, "Bantuan & Dukungan")
                    HorizontalDivider(color = DividerColor)
                    MenuRow(Icons.Default.Info,             "Tentang Aplikasi")
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick  = onLogout,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = StatusCritical),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Keluar", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AdminStatCol(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        fontSize = 13.sp,
        color    = TextHint,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun MenuRow(icon: ImageVector, label: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GreenPrimary)
        )
    }
}
