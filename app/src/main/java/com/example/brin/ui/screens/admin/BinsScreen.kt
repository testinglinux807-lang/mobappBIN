package com.example.brin.ui.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.BinData
import com.example.brin.data.repository.BinRepository
import com.example.brin.ui.screens.shared.BinOnlineDot
import com.example.brin.ui.screens.shared.BinStatusBadge
import com.example.brin.ui.screens.shared.statusColor
import com.example.brin.ui.theme.*
import com.example.brin.util.toUserMessage
import kotlinx.coroutines.launch

@Composable
fun BinsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var bins      by remember { mutableStateOf<List<BinData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<BinData?>(null) }
    var isDeleting   by remember { mutableStateOf(false) }
    var deleteError  by remember { mutableStateOf<String?>(null) }

    fun loadBins() {
        isLoading = true; errorMsg = null
        scope.launch {
            BinRepository.getBins()
                .onSuccess { bins = it }
                .onFailure { errorMsg = it.toUserMessage("Gagal memuat daftar bin.") }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadBins() }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Box(modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White) }
                Text("Manajemen Bin", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(start = 4.dp))
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
                errorMsg != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMsg!!, color = StatusCritical, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { loadBins() }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text("Coba Lagi") }
                    }
                }
                else -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    if (bins.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada bin.", color = TextHint, fontSize = 13.sp)
                        }
                    } else {
                        Text("${bins.size} bin terdaftar", fontSize = 12.sp, color = TextHint, modifier = Modifier.padding(bottom = 10.dp))
                        bins.forEach { bin ->
                            BinManageCard(bin = bin, onEdit = { onEdit(bin.id) }, onDelete = { deleteTarget = bin })
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onAdd,
                containerColor = GreenPrimary, contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Tambah Bin") }
        }
    }

    deleteTarget?.let { bin ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) { deleteTarget = null; deleteError = null } },
            containerColor   = CardBg,
            title = { Text("Hapus Bin?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text("Bin ${bin.nodeId} (${bin.location}) akan dihapus secara permanen.", color = TextSecondary, fontSize = 14.sp)
                    deleteError?.let { Spacer(Modifier.height(8.dp)); Text(it, color = StatusCritical, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true; deleteError = null
                        scope.launch {
                            BinRepository.deleteBin(bin.id)
                                .onSuccess { bins = bins.filter { it.id != bin.id }; deleteTarget = null }
                                .onFailure { deleteError = it.toUserMessage("Gagal menghapus bin. Coba lagi."); isDeleting = false }
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
                TextButton(onClick = { deleteTarget = null; deleteError = null }, enabled = !isDeleting) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun BinManageCard(bin: BinData, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BinOnlineDot(bin.online)
                    Spacer(Modifier.width(6.dp))
                    Text(bin.nodeId, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Text(bin.location, fontSize = 12.sp, color = TextSecondary)
                if (bin.area.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text("Area: ${bin.area}", fontSize = 11.sp, color = TextHint)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BinStatusBadge(bin.status)
                    Spacer(Modifier.width(8.dp))
                    Text("${bin.capacity}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusColor(bin.status))
                }
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit,   contentDescription = "Edit",  tint = TextHint, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = StatusCritical, modifier = Modifier.size(20.dp)) }
        }
    }
}
