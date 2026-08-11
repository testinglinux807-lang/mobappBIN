package com.example.brin.ui.screens.petugas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.local.AppState
import com.example.brin.data.repository.AuthRepository
import com.example.brin.ui.theme.*
import com.example.brin.util.toUserMessage
import kotlinx.coroutines.launch

/** Pengaturan akun petugas: info nama & email + ganti password. */
@Composable
fun PetugasPengaturanScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    var showPassDialog by remember { mutableStateOf(false) }
    var oldPass        by remember { mutableStateOf("") }
    var newPass        by remember { mutableStateOf("") }
    var confirmPass    by remember { mutableStateOf("") }
    var passError      by remember { mutableStateOf<String?>(null) }
    var passSuccess    by remember { mutableStateOf(false) }
    var isSavingPass   by remember { mutableStateOf(false) }

    fun resetDialog() { showPassDialog = false; passError = null; passSuccess = false; oldPass = ""; newPass = ""; confirmPass = "" }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Text("Pengaturan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            SectionLabel("Informasi Akun")
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    InfoRow(Icons.Default.Person, "Nama", AppState.userName)
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    InfoRow(Icons.Default.Email, "Email", AppState.userEmail.ifEmpty { "-" })
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    InfoRow(Icons.Default.Badge, "Peran", if (AppState.userRole == "PETUGAS") "Petugas Lapangan" else AppState.userRole)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Keamanan")
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showPassDialog = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Ganti Password", fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showPassDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingPass) resetDialog() },
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
                        PassField("Password Lama", oldPass) { oldPass = it }
                        PassField("Password Baru (min. 6 char)", newPass) { newPass = it }
                        PassField("Konfirmasi Password Baru", confirmPass) { confirmPass = it }
                        passError?.let { Text(it, color = StatusCritical, fontSize = 12.sp) }
                    }
                }
            },
            confirmButton = {
                if (passSuccess) {
                    Button(onClick = { resetDialog() }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text("Tutup") }
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
                                    .onFailure {
                                        passError = it.toUserMessage(
                                            fallback = "Gagal mengubah password. Coba lagi.",
                                            on401    = "Password lama yang kamu masukkan salah."
                                        )
                                    }
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
                    TextButton(onClick = { if (!isSavingPass) resetDialog() }, enabled = !isSavingPass) {
                        Text("Batal", color = TextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, color = TextHint, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
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
