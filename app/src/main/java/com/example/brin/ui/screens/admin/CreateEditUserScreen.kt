package com.example.brin.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.repository.AreaRepository
import com.example.brin.data.repository.UserRepository
import com.example.brin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditUserScreen(
    userId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val isEdit = userId != null
    val scope  = rememberCoroutineScope()

    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role     by remember { mutableStateOf("PETUGAS") }
    var areaId   by remember { mutableStateOf("") }

    var isLoading  by remember { mutableStateOf(isEdit) }
    var isSaving   by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }

    var roleExpanded by remember { mutableStateOf(false) }

    if (isEdit) {
        LaunchedEffect(userId) {
            UserRepository.getUserById(userId!!).onSuccess { user ->
                name   = user.name
                email  = user.email
                role   = user.role
                areaId = user.areaId ?: ""
            }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Box(modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White) }
                Text(if (isEdit) "Edit Pengguna" else "Tambah Pengguna", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(start = 4.dp))
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GreenPrimary) }
            return
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserFormField("Nama Lengkap", name, "Budi Santoso", onValueChange = { name = it })
            UserFormField("Email", email, "budi@smartbin.id", KeyboardType.Email, onValueChange = { email = it })

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Password${if (isEdit) " (kosongkan jika tidak diubah)" else ""}", fontSize = 12.sp, color = TextSecondary)
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    placeholder = { Text(if (isEdit) "••••••" else "Min. 6 karakter", color = TextHint, fontSize = 14.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary, unfocusedBorderColor = DividerColor,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = GreenPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Role dropdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Role", fontSize = 12.sp, color = TextSecondary)
                ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                    OutlinedTextField(
                        value = role, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary, unfocusedBorderColor = DividerColor,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        listOf("ADMIN", "PETUGAS").forEach { r ->
                            DropdownMenuItem(text = { Text(r) }, onClick = { role = r; roleExpanded = false })
                        }
                    }
                }
            }

            UserFormField("Area ID (opsional)", areaId, "ck...", onValueChange = { areaId = it })

            errorMsg?.let { Text(it, color = StatusCritical, fontSize = 13.sp) }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || (!isEdit && password.isBlank())) {
                        errorMsg = "Nama, email${if (!isEdit) ", dan password" else ""} wajib diisi."
                        return@Button
                    }
                    if (!isEdit && password.length < 6) { errorMsg = "Password minimal 6 karakter."; return@Button }
                    isSaving = true; errorMsg = null
                    val areaIdVal = areaId.trim().takeIf { it.isNotEmpty() }
                    scope.launch {
                        val result = if (isEdit) {
                            UserRepository.updateUser(userId!!, name.trim(), email.trim(), password.takeIf { it.isNotEmpty() }, role, areaIdVal)
                        } else {
                            UserRepository.createUser(name.trim(), email.trim(), password, role, areaIdVal)
                        }
                        result.onSuccess { onSaved() }
                            .onFailure { errorMsg = it.message ?: "Gagal menyimpan"; isSaving = false }
                    }
                },
                enabled = !isSaving,
                colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape   = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (isEdit) "Simpan Perubahan" else "Tambah Pengguna", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UserFormField(label: String, value: String, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextHint, fontSize = 14.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary, unfocusedBorderColor = DividerColor,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = GreenPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
