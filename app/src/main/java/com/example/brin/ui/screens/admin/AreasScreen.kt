package com.example.brin.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.api.ApiArea
import com.example.brin.data.repository.AreaRepository
import com.example.brin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AreasScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var areas      by remember { mutableStateOf<List<ApiArea>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }

    // Create/Edit sheet state
    var showForm   by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<ApiArea?>(null) }  // null = create mode
    var formName   by remember { mutableStateOf("") }
    var formError  by remember { mutableStateOf<String?>(null) }
    var isSaving   by remember { mutableStateOf(false) }

    // Delete state
    var deleteTarget by remember { mutableStateOf<ApiArea?>(null) }
    var isDeleting   by remember { mutableStateOf(false) }
    var deleteError  by remember { mutableStateOf<String?>(null) }

    fun loadAreas() {
        isLoading = true; errorMsg = null
        scope.launch {
            AreaRepository.getAreas().onSuccess { areas = it }.onFailure { errorMsg = it.message }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadAreas() }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Box(modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White) }
                Text("Manajemen Area/Zona", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(start = 4.dp))
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GreenPrimary) }
                errorMsg != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMsg!!, color = StatusCritical, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { loadAreas() }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text("Coba Lagi") }
                    }
                }
                else -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 80.dp)) {
                    if (areas.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada area.", color = TextHint, fontSize = 13.sp)
                        }
                    } else {
                        Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                areas.forEachIndexed { index, area ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(area.name, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { editTarget = area; formName = area.name; formError = null; showForm = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextHint, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { deleteTarget = area }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = StatusCritical, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    if (index < areas.lastIndex) HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { editTarget = null; formName = ""; formError = null; showForm = true },
                containerColor = GreenPrimary, contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Tambah Area") }
        }
    }

    // Create/Edit dialog
    if (showForm) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showForm = false },
            containerColor   = CardBg,
            title = { Text(if (editTarget != null) "Edit Area" else "Tambah Area", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text("Nama Area", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = formName, onValueChange = { formName = it },
                        placeholder = { Text("Area Selatan", color = TextHint, fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary, unfocusedBorderColor = DividerColor,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = GreenPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    formError?.let { Spacer(Modifier.height(8.dp)); Text(it, color = StatusCritical, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formName.trim().length < 3) { formError = "Nama minimal 3 karakter."; return@Button }
                        isSaving = true; formError = null
                        scope.launch {
                            val result = if (editTarget != null)
                                AreaRepository.updateArea(editTarget!!.id, formName.trim())
                            else
                                AreaRepository.createArea(formName.trim())
                            result.onSuccess {
                                loadAreas(); showForm = false
                            }.onFailure { formError = it.message ?: "Gagal menyimpan"; isSaving = false }
                            isSaving = false
                        }
                    },
                    enabled = !isSaving,
                    colors  = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForm = false }, enabled = !isSaving) { Text("Batal", color = TextSecondary) }
            }
        )
    }

    // Delete confirmation
    deleteTarget?.let { area ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) { deleteTarget = null; deleteError = null } },
            containerColor   = CardBg,
            title = { Text("Hapus Area?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text("\"${area.name}\" akan dihapus. Gagal jika masih ada bin/user yang terhubung.", color = TextSecondary, fontSize = 14.sp)
                    deleteError?.let { Spacer(Modifier.height(8.dp)); Text(it, color = StatusCritical, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true; deleteError = null
                        scope.launch {
                            AreaRepository.deleteArea(area.id)
                                .onSuccess { areas = areas.filter { it.id != area.id }; deleteTarget = null }
                                .onFailure { deleteError = it.message ?: "Gagal menghapus"; isDeleting = false }
                            isDeleting = false
                        }
                    },
                    enabled = !isDeleting,
                    colors  = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                ) {
                    if (isDeleting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null; deleteError = null }, enabled = !isDeleting) { Text("Batal", color = TextSecondary) }
            }
        )
    }
}
