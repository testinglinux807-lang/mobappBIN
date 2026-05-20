package com.example.brin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.brin.ui.theme.*

@Composable
fun ProfileScreen() {
    Column(
        modifier             = Modifier.fillMaxSize().background(AppBackground),
        horizontalAlignment  = Alignment.CenterHorizontally
    ) {
        Surface(color = CardBg, modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(GreenSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null,
                        tint = GreenPrimary, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Andrian", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Administrator · Sistem BRIN", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatColumn("248", "Total Bin")
                    StatColumn("12", "Petugas Aktif")
                    StatColumn("6", "Zona Aktif")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            shape    = RoundedCornerShape(14.dp),
            color    = CardBg,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                ProfileMenuItem(Icons.Default.Notifications, "Notifikasi")
                HorizontalDivider(color = DividerColor)
                ProfileMenuItem(Icons.Default.BarChart, "Laporan Aktivitas")
                HorizontalDivider(color = DividerColor)
                ProfileMenuItem(Icons.Default.Settings, "Pengaturan")
                HorizontalDivider(color = DividerColor)
                ProfileMenuItem(Icons.AutoMirrored.Filled.Help, "Bantuan")
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick  = { },
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = StatusCritical),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null,
                tint = StatusCritical, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Keluar", color = StatusCritical, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}
