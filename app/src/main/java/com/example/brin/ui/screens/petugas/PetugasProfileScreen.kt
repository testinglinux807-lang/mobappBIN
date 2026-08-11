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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.local.AppState
import com.example.brin.data.repository.BinRepository
import com.example.brin.data.repository.PickupRepository
import com.example.brin.ui.theme.*

@Composable
fun PetugasProfileScreen(
    onLogout: () -> Unit = {},
    onHelp: () -> Unit = {},
    onHistory: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val user = AppState.currentUser
    val name = AppState.userName
    val roleLabel = if (AppState.userRole == "PETUGAS") "Petugas Lapangan" else "Admin"
    val subtitle = roleLabel + (user?.area?.name?.let { " · $it" } ?: "")
    val avatarInitial = name.trim().firstOrNull()?.uppercase() ?: "?"

    var totalPickup by remember { mutableIntStateOf(0) }
    var totalBin    by remember { mutableIntStateOf(0) }
    var efisiensi   by remember { mutableStateOf("-") }
    LaunchedEffect(Unit) {
        PickupRepository.getPickups().onSuccess { pickups ->
            // scope ke petugas yang login (backend bisa saja sudah memfilter — ini pengaman)
            val mine = user?.id?.let { id -> pickups.filter { it.petugasId == id } } ?: pickups
            totalPickup = mine.size
            val selesai = mine.count { it.status == "SELESAI" }
            efisiensi = if (mine.isNotEmpty()) "${selesai * 100 / mine.size}%" else "-"
        }
        BinRepository.getBins().onSuccess { totalBin = it.size }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(avatarInitial, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                ProfileStat("$totalPickup", "Total Pickup")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                ProfileStat("$totalBin", "Bin / Rute")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                ProfileStat(efisiensi, "Efisiensi")
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))

            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    ProfileMenuItem(Icons.Default.BarChart, "Riwayat Pickup", onClick = onHistory)
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    ProfileMenuItem(Icons.Default.Settings, "Pengaturan", onClick = onSettings)
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    ProfileMenuItem(Icons.AutoMirrored.Filled.Help, "Bantuan", onClick = onHelp)
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
private fun ProfileMenuItem(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}
