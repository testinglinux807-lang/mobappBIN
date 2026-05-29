package com.example.brin.ui.screens.admin

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.brin.data.local.AppState
import com.example.brin.data.repository.AuthRepository
import com.example.brin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onLogout: () -> Unit = {}, onUsers: () -> Unit = {}, onAreas: () -> Unit = {}) {
    val scope         = rememberCoroutineScope()
    val context       = LocalContext.current
    var notifEnabled  by remember { mutableStateOf(true) }
    var criticalAlert by remember { mutableStateOf(true) }
    var autoRefresh   by remember { mutableStateOf(true) }

    // Change password dialog state
    var showPassDialog by remember { mutableStateOf(false) }
    var oldPass        by remember { mutableStateOf("") }
    var newPass        by remember { mutableStateOf("") }
    var confirmPass    by remember { mutableStateOf("") }
    var passError      by remember { mutableStateOf<String?>(null) }
    var passSuccess    by remember { mutableStateOf(false) }
    var isSavingPass   by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(AppState.userName.firstOrNull()?.uppercase() ?: "U", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text(AppState.userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(AppState.userEmail, fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
            Spacer(Modifier.height(22.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                SettingStat("248", "Total Bin")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.18f)))
                SettingStat("12", "Petugas")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.18f)))
                SettingStat("6", "Zona Aktif")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.18f)))
                SettingStat("99%", "Uptime")
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {
            Spacer(Modifier.height(20.dp))
            SectionLabel("Notifikasi")
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    ToggleRow(Icons.Default.Notifications, "Notifikasi Push",   notifEnabled)  { notifEnabled  = it }
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    ToggleRow(Icons.Default.Warning,       "Alert Bin Kritis",  criticalAlert) { criticalAlert = it }
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    ToggleRow(Icons.Default.Refresh,       "Auto Refresh Data", autoRefresh)   { autoRefresh   = it }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Manajemen Sistem")
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    MenuRow(Icons.Default.People,        "Manajemen Petugas", onClick = onUsers)
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    MenuRow(Icons.Default.LocationOn,    "Manajemen Zona",    onClick = onAreas)
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    MenuRow(Icons.Default.DeleteOutline, "Manajemen Bin")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    MenuRow(Icons.Default.Download,      "Export Laporan")
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Akun")
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                MenuRow(Icons.Default.Lock, "Ganti Password", onClick = { showPassDialog = true })
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Lainnya")
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    MenuRow(Icons.AutoMirrored.Filled.Help, "Bantuan & Dukungan")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    MenuRow(Icons.Default.Info, "Tentang Aplikasi")
                }
            }

            Spacer(Modifier.height(20.dp))
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

    if (showPassDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingPass) { showPassDialog = false; passError = null; passSuccess = false; oldPass = ""; newPass = ""; confirmPass = "" } },
            containerColor   = CardBg,
            title = { Text("Ganti Password", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (passSuccess) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusNormal, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Password berhasil diubah.", color = StatusNormal, fontSize = 13.sp)
                        }
                    } else {
                        PassField("Password Lama", oldPass)        { oldPass = it }
                        PassField("Password Baru (min. 6 char)", newPass) { newPass = it }
                        PassField("Konfirmasi Password Baru", confirmPass) { confirmPass = it }
                        passError?.let { Text(it, color = StatusCritical, fontSize = 12.sp) }
                    }
                }
            },
            confirmButton = {
                if (passSuccess) {
                    Button(onClick = { showPassDialog = false; passSuccess = false; oldPass = ""; newPass = ""; confirmPass = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text("Tutup") }
                } else {
                    Button(
                        onClick = {
                            passError = when {
                                oldPass.isBlank() || newPass.isBlank() || confirmPass.isBlank() -> "Semua field wajib diisi."
                                newPass.length < 6 -> "Password baru minimal 6 karakter."
                                newPass == oldPass  -> "Password baru harus berbeda dengan yang lama."
                                newPass != confirmPass -> "Konfirmasi password tidak cocok."
                                else -> null
                            }
                            if (passError != null) return@Button
                            isSavingPass = true
                            scope.launch {
                                AuthRepository.changePassword(oldPass, newPass)
                                    .onSuccess { passSuccess = true; oldPass = ""; newPass = ""; confirmPass = "" }
                                    .onFailure { passError = it.message ?: "Gagal mengubah password" }
                                isSavingPass = false
                            }
                        },
                        enabled = !isSavingPass,
                        colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        if (isSavingPass) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Simpan")
                    }
                }
            },
            dismissButton = {
                if (!passSuccess) {
                    TextButton(onClick = { showPassDialog = false; passError = null; oldPass = ""; newPass = ""; confirmPass = "" }, enabled = !isSavingPass) {
                        Text("Batal", color = TextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun PassField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary, unfocusedBorderColor = DividerColor,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = GreenPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
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
    Text(text, fontSize = 12.sp, color = TextHint, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GreenPrimary))
    }
}
