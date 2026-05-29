package com.example.brin.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.TaskStatus
import com.example.brin.ui.screens.shared.taskStatusColor
import com.example.brin.ui.screens.shared.taskStatusLabel
import com.example.brin.ui.theme.*
import java.util.Calendar

private data class Petugas(val id: String, val nama: String, val zona: String, val avatar: String)
private data class JadwalItem(val petugasId: String, val ruteId: String, val zona: String, val binCount: Int, val jamMulai: String, val jamSelesai: String, val status: TaskStatus)

private val petugasList = listOf(
    Petugas("P-01", "Andrian", "Zona A", "A"),
    Petugas("P-02", "Budi",    "Zona B", "B"),
    Petugas("P-03", "Cahyo",   "Zona C", "C"),
    Petugas("P-04", "Deni",    "Zona D", "D"),
    Petugas("P-05", "Eko",     "Zona E", "E"),
)
private val ruteOptions = listOf("TRK-01", "TRK-02", "TRK-03", "TRK-04", "TRK-05", "TRK-06")
private val namaHari = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
private data class TanggalItem(val tanggal: Int, val hari: String)

private fun getDaysInMonth(year: Int, month: Int): List<TanggalItem> {
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return (1..maxDay).map { day ->
        cal.set(Calendar.DAY_OF_MONTH, day)
        TanggalItem(day, namaHari[cal.get(Calendar.DAY_OF_WEEK) - 1])
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen() {
    val cal        = remember { Calendar.getInstance() }
    val today      = cal.get(Calendar.DAY_OF_MONTH)
    val year       = cal.get(Calendar.YEAR)
    val month      = cal.get(Calendar.MONTH)
    val bulan      = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("id")).format(cal.time) }
    val days       = remember { getDaysInMonth(year, month) }
    val todayIndex = remember { (today - 1).coerceIn(0, days.lastIndex) }
    val listState  = rememberLazyListState(initialFirstVisibleItemIndex = (todayIndex - 2).coerceAtLeast(0))

    var selectedDate   by remember { mutableIntStateOf(todayIndex) }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showSheet      by remember { mutableStateOf(false) }
    var editingItem    by remember { mutableStateOf<JadwalItem?>(null) }

    val jadwalList = remember {
        mutableStateListOf(
            JadwalItem("P-01", "TRK-01", "Zona A", 6, "08.00", "10.00", TaskStatus.PENDING),
            JadwalItem("P-02", "TRK-02", "Zona B", 4, "09.30", "11.00", TaskStatus.IN_PROGRESS),
            JadwalItem("P-03", "TRK-03", "Zona C", 6, "07.45", "09.30", TaskStatus.DONE),
            JadwalItem("P-04", "TRK-04", "Zona D", 5, "11.00", "13.00", TaskStatus.PENDING),
            JadwalItem("P-05", "TRK-05", "Zona E", 3, "13.00", "14.30", TaskStatus.PENDING),
        )
    }

    val filters  = listOf("Semua", "Pending", "Proses", "Selesai")
    val filtered = remember(selectedFilter, jadwalList.size) {
        when (selectedFilter) {
            "Pending" -> jadwalList.filter { it.status == TaskStatus.PENDING }
            "Proses"  -> jadwalList.filter { it.status == TaskStatus.IN_PROGRESS }
            "Selesai" -> jadwalList.filter { it.status == TaskStatus.DONE }
            else      -> jadwalList.toList()
        }
    }

    if (showSheet) {
        TambahJadwalSheet(onDismiss = { showSheet = false }, onSave = { item -> jadwalList.add(item); showSheet = false })
    }
    editingItem?.let { original ->
        TambahJadwalSheet(
            initialItem = original,
            onDismiss = { editingItem = null },
            onSave = { updated -> val idx = jadwalList.indexOf(original); if (idx >= 0) jadwalList[idx] = updated; editingItem = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding().padding(bottom = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Jadwal Petugas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(bulan, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                }
                IconButton(onClick = { showSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah jadwal", tint = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                itemsIndexed(days) { index, day ->
                    val isSelected = selectedDate == index
                    val isToday    = index == todayIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { selectedDate = index }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(day.hari, fontSize = 11.sp, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f))
                        Spacer(Modifier.height(4.dp))
                        Text("${day.tanggal}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(2.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(
                            when { isSelected -> Color.White; isToday -> Color.White.copy(alpha = 0.6f); else -> Color.Transparent }
                        ))
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { f ->
                val isActive = selectedFilter == f
                Surface(shape = RoundedCornerShape(20.dp), color = if (isActive) GreenPrimary else CardBg) {
                    Text(f, fontSize = 12.sp, color = if (isActive) Color.White else TextSecondary,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.clickable { selectedFilter = f }.padding(horizontal = 14.dp, vertical = 7.dp))
                }
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ada jadwal", color = TextHint, fontSize = 14.sp)
                }
            } else {
                filtered.forEach { jadwal ->
                    val petugas = petugasList.find { it.id == jadwal.petugasId }
                    if (petugas != null) JadwalCard(jadwal = jadwal, petugas = petugas, onEdit = { editingItem = jadwal }, onDelete = { jadwalList.remove(jadwal) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TambahJadwalSheet(initialItem: JadwalItem? = null, onDismiss: () -> Unit, onSave: (JadwalItem) -> Unit) {
    val isEditMode = initialItem != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPetugas by remember { mutableStateOf(petugasList.find { it.id == initialItem?.petugasId } ?: petugasList[0]) }
    var petugasExpanded by remember { mutableStateOf(false) }
    var selectedRute    by remember { mutableStateOf(initialItem?.ruteId ?: ruteOptions[0]) }
    var ruteExpanded    by remember { mutableStateOf(false) }
    var binCount        by remember { mutableStateOf(initialItem?.binCount?.toString() ?: "") }
    var jamMulai        by remember { mutableStateOf(initialItem?.jamMulai ?: "") }
    var jamSelesai      by remember { mutableStateOf(initialItem?.jamSelesai ?: "") }
    var errorMsg        by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardBg, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isEditMode) "Edit Jadwal" else "Tambah Jadwal Baru", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextHint, modifier = Modifier.size(22.dp).clickable { onDismiss() })
            }
            HorizontalDivider(color = DividerColor)

            FormLabel("Petugas")
            ExposedDropdownMenuBox(expanded = petugasExpanded, onExpandedChange = { petugasExpanded = it }) {
                OutlinedTextField(
                    value = "${selectedPetugas.nama} · ${selectedPetugas.zona}", onValueChange = {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = petugasExpanded) },
                    shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                    leadingIcon = {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(GreenSurface), contentAlignment = Alignment.Center) {
                            Text(selectedPetugas.avatar, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                    }
                )
                ExposedDropdownMenu(expanded = petugasExpanded, onDismissRequest = { petugasExpanded = false }, containerColor = CardBg) {
                    petugasList.forEach { p ->
                        DropdownMenuItem(
                            text = { Text("${p.nama} · ${p.zona}", fontSize = 14.sp, color = TextPrimary) },
                            onClick = { selectedPetugas = p; petugasExpanded = false },
                            leadingIcon = {
                                Box(Modifier.size(28.dp).clip(CircleShape).background(GreenSurface), contentAlignment = Alignment.Center) {
                                    Text(p.avatar, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                }
                            }
                        )
                    }
                }
            }

            FormLabel("Rute / Truk")
            ExposedDropdownMenuBox(expanded = ruteExpanded, onExpandedChange = { ruteExpanded = it }) {
                OutlinedTextField(
                    value = selectedRute, onValueChange = {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ruteExpanded) },
                    shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                    leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp)) }
                )
                ExposedDropdownMenu(expanded = ruteExpanded, onDismissRequest = { ruteExpanded = false }, containerColor = CardBg) {
                    ruteOptions.forEach { r ->
                        DropdownMenuItem(text = { Text(r, fontSize = 14.sp, color = TextPrimary) }, onClick = { selectedRute = r; ruteExpanded = false })
                    }
                }
            }

            FormLabel("Jumlah Bin")
            OutlinedTextField(
                value = binCount, onValueChange = { if (it.length <= 3) binCount = it.filter { c -> c.isDigit() } },
                placeholder = { Text("Contoh: 6", color = TextHint, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp)) },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    FormLabel("Jam Mulai")
                    OutlinedTextField(value = jamMulai, onValueChange = { jamMulai = formatJam(it) }, placeholder = { Text("08.00", color = TextHint, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp)) }, singleLine = true)
                }
                Column(modifier = Modifier.weight(1f)) {
                    FormLabel("Jam Selesai")
                    OutlinedTextField(value = jamSelesai, onValueChange = { jamSelesai = formatJam(it) }, placeholder = { Text("10.00", color = TextHint, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp)) }, singleLine = true)
                }
            }

            if (errorMsg.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(8.dp), color = StatusCritical.copy(alpha = 0.1f)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(errorMsg, fontSize = 12.sp, color = StatusCritical)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary), border = ButtonDefaults.outlinedButtonBorder(enabled = true)) {
                    Text("Batal", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        errorMsg = ""
                        val bins = binCount.toIntOrNull()
                        when {
                            jamMulai.isEmpty()           -> errorMsg = "Jam mulai harus diisi"
                            jamSelesai.isEmpty()          -> errorMsg = "Jam selesai harus diisi"
                            bins == null || bins <= 0    -> errorMsg = "Jumlah bin tidak valid"
                            else -> onSave(JadwalItem(selectedPetugas.id, selectedRute, selectedPetugas.zona, bins, jamMulai, jamSelesai, initialItem?.status ?: TaskStatus.PENDING))
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Simpan", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatJam(input: String): String {
    val digits = input.filter { it.isDigit() }.take(4)
    return when { digits.length <= 2 -> digits; else -> "${digits.substring(0, 2)}.${digits.substring(2)}" }
}

@Composable
private fun FormLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenPrimary, unfocusedBorderColor = DividerColor,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = GreenPrimary,
    focusedContainerColor = AppBackground, unfocusedContainerColor = AppBackground
)

@Composable
private fun JadwalCard(jadwal: JadwalItem, petugas: Petugas, onEdit: () -> Unit, onDelete: () -> Unit) {
    val statusColor  = taskStatusColor(jadwal.status)
    val statusLabel  = taskStatusLabel(jadwal.status)
    var showConfirm  by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = CardBg, shape = RoundedCornerShape(16.dp),
            icon = {
                Box(Modifier.size(44.dp).clip(CircleShape).background(StatusCritical.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(22.dp))
                }
            },
            title = { Text("Hapus Jadwal?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Jadwal ${petugas.nama} · ${jadwal.ruteId} akan dihapus secara permanen.", fontSize = 13.sp, color = TextSecondary) },
            confirmButton = {
                Button(onClick = { showConfirm = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = StatusCritical), shape = RoundedCornerShape(10.dp)) {
                    Text("Hapus", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirm = false }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) {
                    Text("Batal")
                }
            }
        )
    }

    Surface(shape = RoundedCornerShape(16.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(GreenSurface), contentAlignment = Alignment.Center) {
                        Text(petugas.avatar, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(petugas.nama, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(petugas.zona, fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                        Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit jadwal", tint = GreenMedium, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus jadwal", tint = StatusCritical, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoChip(Icons.Default.LocalShipping, jadwal.ruteId)
                InfoChip(Icons.Default.DeleteOutline,  "${jadwal.binCount} bin")
                InfoChip(Icons.Default.Schedule,       "${jadwal.jamMulai} – ${jadwal.jamSelesai}")
            }
            if (jadwal.status == TaskStatus.IN_PROGRESS) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { 0.5f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), color = GreenPrimary, trackColor = DividerColor)
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextHint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}
