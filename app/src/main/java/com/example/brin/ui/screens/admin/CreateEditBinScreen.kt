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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.api.ApiArea
import com.example.brin.data.repository.AreaRepository
import com.example.brin.data.repository.BinRepository
import com.example.brin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditBinScreen(
    binId: String?,        // null = create mode, non-null = edit mode
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val isEdit = binId != null
    val scope  = rememberCoroutineScope()

    var nodeId   by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var latStr   by remember { mutableStateOf("") }
    var lngStr   by remember { mutableStateOf("") }
    var areaId   by remember { mutableStateOf("") }

    // Threshold fields (edit mode)
    var weightThr  by remember { mutableStateOf("") }
    var volumeThr  by remember { mutableStateOf("") }
    var gasThr     by remember { mutableStateOf("") }
    var batteryThr by remember { mutableStateOf("") }

    var isLoading    by remember { mutableStateOf(isEdit) }
    var isSaving     by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }
    var areas        by remember { mutableStateOf<List<ApiArea>>(emptyList()) }
    var areaExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AreaRepository.getAreas().onSuccess { areas = it }
    }

    // Load existing bin data in edit mode
    if (isEdit) {
        LaunchedEffect(binId) {
            BinRepository.getBinById(binId!!).onSuccess { bin ->
                nodeId   = bin.nodeId
                location = bin.location
                latStr   = bin.lat.toString()
                lngStr   = bin.lng.toString()
                areaId   = bin.area
            }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Text(
                    text = if (isEdit) "Edit Bin" else "Tambah Bin",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            return
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("Informasi Bin")

            FormField("Node ID", nodeId, "bin-001", onValueChange = { nodeId = it })
            FormField("Lokasi", location, "Lobby Gedung A", onValueChange = { location = it })
            FormField("Latitude", latStr, "-6.9175", KeyboardType.Decimal, onValueChange = { latStr = it })
            FormField("Longitude", lngStr, "107.6191", KeyboardType.Decimal, onValueChange = { lngStr = it })
            // Area dropdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Area (opsional)", fontSize = 12.sp, color = TextSecondary)
                ExposedDropdownMenuBox(expanded = areaExpanded, onExpandedChange = { areaExpanded = it }) {
                    val displayName = areas.find { it.id == areaId }?.name ?: if (areaId.isNotEmpty()) areaId else "Tidak ada"
                    OutlinedTextField(
                        value = displayName, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary, unfocusedBorderColor = DividerColor,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = areaExpanded, onDismissRequest = { areaExpanded = false }) {
                        DropdownMenuItem(text = { Text("Tidak ada", color = TextHint) }, onClick = { areaId = ""; areaExpanded = false })
                        areas.forEach { area ->
                            DropdownMenuItem(text = { Text(area.name) }, onClick = { areaId = area.id; areaExpanded = false })
                        }
                    }
                }
            }

            if (isEdit) {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Threshold Alert")
                Text(
                    "Kosongkan untuk tidak mengubah threshold saat ini.",
                    fontSize = 12.sp, color = TextHint, modifier = Modifier.padding(bottom = 4.dp)
                )
                FormField("Weight threshold (kg)", weightThr, "50", KeyboardType.Decimal, onValueChange = { weightThr = it })
                FormField("Volume threshold (%)", volumeThr, "85", KeyboardType.Number, onValueChange = { volumeThr = it })
                FormField("Gas threshold (ppm)", gasThr, "300", KeyboardType.Decimal, onValueChange = { gasThr = it })
                FormField("Battery threshold (%)", batteryThr, "20", KeyboardType.Number, onValueChange = { batteryThr = it })
            }

            errorMsg?.let {
                Text(it, color = StatusCritical, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val lat = latStr.toDoubleOrNull()
                    val lng = lngStr.toDoubleOrNull()
                    if (nodeId.isBlank() || location.isBlank() || lat == null || lng == null) {
                        errorMsg = "Node ID, lokasi, lat, dan lng wajib diisi dengan benar."
                        return@Button
                    }
                    isSaving = true
                    errorMsg = null
                    scope.launch {
                        val areaIdVal = areaId.trim().takeIf { it.isNotEmpty() }
                        val result = if (isEdit) {
                            BinRepository.updateBin(binId!!, nodeId.trim(), location.trim(), lat, lng, areaIdVal)
                        } else {
                            BinRepository.createBin(nodeId.trim(), location.trim(), lat, lng, areaIdVal)
                        }

                        result.onSuccess {
                            // Save threshold separately if in edit mode and any field is filled
                            if (isEdit) {
                                val wt = weightThr.toDoubleOrNull()
                                val vt = volumeThr.toIntOrNull()
                                val gt = gasThr.toDoubleOrNull()
                                val bt = batteryThr.toIntOrNull()
                                if (wt != null || vt != null || gt != null || bt != null) {
                                    BinRepository.setThreshold(binId!!, wt, vt, gt, bt)
                                }
                            }
                            onSaved()
                        }.onFailure { e ->
                            errorMsg = e.message ?: "Gagal menyimpan"
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isEdit) "Simpan Perubahan" else "Tambah Bin", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
}

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextHint, fontSize = 14.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GreenPrimary,
                unfocusedBorderColor = DividerColor,
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary,
                cursorColor          = GreenPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
