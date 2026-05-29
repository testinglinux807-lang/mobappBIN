package com.example.brin.ui.screens.petugas

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
import androidx.compose.runtime.Composable
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
fun PetugasProfileScreen(onLogout: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text("A", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text("Andrian", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Petugas Lapangan · Zona A", fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                ProfileStat("24", "Total Pickup")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                ProfileStat("6", "Bin / Rute")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                ProfileStat("98%", "Efisiensi")
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {
            Spacer(Modifier.height(16.dp))

            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    ProfileMenuItem(Icons.Default.Notifications, "Notifikasi")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    ProfileMenuItem(Icons.Default.BarChart, "Riwayat Pickup")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    ProfileMenuItem(Icons.Default.Settings, "Pengaturan")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    ProfileMenuItem(Icons.AutoMirrored.Filled.Help, "Bantuan")
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onLogout).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Keluar", fontSize = 14.sp, color = StatusCritical, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}
