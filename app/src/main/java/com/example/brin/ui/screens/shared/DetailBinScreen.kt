package com.example.brin.ui.screens.shared

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.rememberCoroutineScope
import com.example.brin.data.BinData
import com.example.brin.data.BinStatus
import com.example.brin.data.local.AppState
import com.example.brin.data.repository.AlertRepository
import com.example.brin.data.repository.BinRepository
import com.example.brin.data.repository.PickupRepository
import com.example.brin.data.websocket.WebSocketManager
import com.example.brin.data.websocket.WsEvent
import com.example.brin.ui.theme.*
import com.example.brin.util.LocationHelper
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun DetailBinScreen(
    binId: String,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDeleted: (() -> Unit)? = null
) {
    val scope         = rememberCoroutineScope()
    val context       = LocalContext.current
    val isAdmin       = AppState.userRole == "ADMIN"
    var bin           by remember { mutableStateOf<BinData?>(null) }
    var showQrDialog      by remember { mutableStateOf(false) }
    var showDelDialog     by remember { mutableStateOf(false) }
    var isDeleting        by remember { mutableStateOf(false) }
    var deleteError       by remember { mutableStateOf<String?>(null) }
    var showPickupDialog  by remember { mutableStateOf(false) }
    var isCompletingPickup by remember { mutableStateOf(false) }
    var pickupError       by remember { mutableStateOf<String?>(null) }
    var pickupSuccess     by remember { mutableStateOf(false) }
    // Status alert dari ALERT (bukan volume), supaya konfirmasi manual yang me-resolve
    // alert langsung menghilangkan banner peringatan di sini.
    var hasOpenAlert  by remember { mutableStateOf(false) }
    var openAlertMsg  by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(binId) {
        BinRepository.getBinById(binId).onSuccess { bin = it }
        AlertRepository.getAlerts(resolved = false).onSuccess { list ->
            val a = list.firstOrNull { it.binRefId == binId && (it.rawType == "FULL_VOLUME" || it.rawType == "FULL_WEIGHT") }
            hasOpenAlert = a != null
            openAlertMsg = a?.message
        }
    }

    // Live update via WebSocket: alert resolve/baru + pembacaan sensor terbaru.
    LaunchedEffect(binId) {
        WebSocketManager.events.collect { event ->
            when (event) {
                is WsEvent.AlertResolved -> if (event.binId == binId) { hasOpenAlert = false; openAlertMsg = null }
                is WsEvent.AlertNew -> if (event.binId == binId && (event.type == "FULL_VOLUME" || event.type == "FULL_WEIGHT")) {
                    hasOpenAlert = true; openAlertMsg = event.message
                }
                is WsEvent.BinUpdate -> if (event.binId == binId) bin = bin?.copy(
                    capacity   = event.volume,
                    battery    = event.battery,
                    gas        = event.gas,
                    lastUpdate = "Baru saja",
                    status     = when { event.volume >= 90 -> BinStatus.CRITICAL; event.volume >= 70 -> BinStatus.NEED_PICKUP; else -> BinStatus.NORMAL }
                )
                else -> Unit
            }
        }
    }

    val currentBin = bin  // local snapshot — avoids smart-cast issue on delegated property

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        val sColor = if (currentBin != null) statusColor(currentBin.status) else GreenPrimary
        val sLabel = if (currentBin != null) statusLabel(currentBin.status) else ""

        Column(
            modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding().padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White.copy(alpha = 0.7f))
                }
            }

            if (currentBin != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(currentBin.nodeId, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(2.dp))
                        Text(currentBin.location, fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (currentBin.online) Color(0xFF4ADE80) else Color(0xFFBDBDBD)))
                            Spacer(Modifier.width(6.dp))
                            Text(if (currentBin.online) "Online" else "Offline", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = sColor.copy(alpha = 0.25f)) {
                        Text(sLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White,
                            maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kapasitas", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        Text("${currentBin.capacity}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { currentBin.capacity / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color.White, trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        if (currentBin == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            return
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {
            if (hasOpenAlert && currentBin.status != BinStatus.NORMAL) {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(10.dp)).background(CardBg)) {
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(sColor))
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = sColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Kapasitas ${currentBin.capacity}% · ${currentBin.alertText.ifEmpty { "Perlu diangkut" }}", fontSize = 13.sp, color = sColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Informasi Bin", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    InfoRow(Icons.Default.LocationOn,  "Lokasi",          currentBin.location)
                    if (currentBin.area.isNotEmpty()) {
                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                        InfoRow(Icons.Default.Place,       "Area",            currentBin.area)
                    }
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    InfoRow(Icons.Default.Schedule,    "Terakhir Update", currentBin.lastUpdate)
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    InfoRow(Icons.Default.Warning,     "Status",          sLabel, valueColor = sColor)
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    InfoRow(Icons.Default.BatteryFull, "Baterai",         "${currentBin.battery}%")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    InfoRow(Icons.Default.Science,      "Gas",             "${currentBin.gas.toInt()} ppm")
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Aksi Cepat", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    if (isAdmin) {
                        FlatAction("Generate QR", GreenPrimary, bold = true) { showQrDialog = true }
                    } else {
                        FlatAction("Pickup Selesai", StatusWarning, bold = true) { showPickupDialog = true }
                    }
                    if (isAdmin && onEdit != null) {
                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                        FlatAction("Edit Bin", TextPrimary) { onEdit() }
                    }
                    if (isAdmin && onDeleted != null) {
                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                        FlatAction("Hapus Bin", StatusCritical) { showDelDialog = true }
                    }
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 14.dp))
                    FlatAction("Navigasi ke Lokasi", TextPrimary) { }
                }
            }
        }
    }

    if (showPickupDialog && currentBin != null) {
        AlertDialog(
            onDismissRequest = { if (!isCompletingPickup) { showPickupDialog = false; pickupError = null; pickupSuccess = false } },
            containerColor   = CardBg,
            title = { Text("Pickup Selesai?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                if (pickupSuccess) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusNormal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pickup berhasil dicatat. Menunggu konfirmasi sensor.", color = StatusNormal, fontSize = 13.sp)
                    }
                } else {
                    Column {
                        Text("Konfirmasi bahwa pickup bin ${currentBin.nodeId} (${currentBin.location}) telah selesai dilakukan.", color = TextSecondary, fontSize = 14.sp)
                        pickupError?.let { Spacer(Modifier.height(8.dp)); Text(it, color = StatusCritical, fontSize = 13.sp) }
                    }
                }
            },
            confirmButton = {
                if (pickupSuccess) {
                    Button(onClick = { showPickupDialog = false; pickupSuccess = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text("Tutup") }
                } else {
                    Button(
                        onClick = {
                            isCompletingPickup = true; pickupError = null
                            scope.launch {
                                val loc = LocationHelper.getCurrentLocation(context)
                                PickupRepository.completePickup(currentBin.id, loc?.lat, loc?.lng)
                                    .onSuccess { pickupSuccess = true }
                                    .onFailure { pickupError = it.message ?: "Gagal mencatat pickup" }
                                isCompletingPickup = false
                            }
                        },
                        enabled = !isCompletingPickup,
                        colors  = ButtonDefaults.buttonColors(containerColor = StatusWarning)
                    ) {
                        if (isCompletingPickup) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Konfirmasi")
                    }
                }
            },
            dismissButton = {
                if (!pickupSuccess) {
                    TextButton(onClick = { showPickupDialog = false; pickupError = null }, enabled = !isCompletingPickup) {
                        Text("Batal", color = TextSecondary)
                    }
                }
            }
        )
    }

    if (showQrDialog && currentBin != null) {
        QrDialog(bin = currentBin, onDismiss = { showQrDialog = false })
    }

    if (showDelDialog && currentBin != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDelDialog = false },
            containerColor   = CardBg,
            title = { Text("Hapus Bin?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text("Bin ${currentBin.nodeId} (${currentBin.location}) akan dihapus secara permanen.", color = TextSecondary, fontSize = 14.sp)
                    deleteError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = StatusCritical, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        deleteError = null
                        scope.launch {
                            BinRepository.deleteBin(currentBin.id)
                                .onSuccess { onDeleted?.invoke() }
                                .onFailure { deleteError = it.message ?: "Gagal menghapus"; isDeleting = false }
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
                TextButton(onClick = { showDelDialog = false }, enabled = !isDeleting) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextHint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = valueColor, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f))
    }
}

@Composable
private fun FlatAction(label: String, color: Color, bold: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = color, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun QrDialog(bin: BinData, onDismiss: () -> Unit) {
    val qrContent = "brin://pickup?binId=${bin.id}&lat=${bin.lat}&lng=${bin.lng}"
    val qrBitmap  = remember(qrContent) { generateQrBitmap(qrContent) }
    val sColor    = statusColor(bin.status)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = CardBg) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("QR Code", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(bin.nodeId, fontSize = 13.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextHint) }
                }
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR Code ${bin.id}",
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp))
                }
                Spacer(Modifier.height(14.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = AppBackground) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        QrInfoRow(Icons.Default.LocationOn, bin.location)
                        QrInfoRow(Icons.Default.MyLocation, "${bin.lat}, ${bin.lng}")
                        QrInfoRow(Icons.Default.Info, statusLabel(bin.status), color = sColor)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Tempel QR ini di tempat sampah. Petugas scan\ndi lokasi untuk langsung mencatat pickup.",
                    fontSize = 11.sp, color = TextHint, textAlign = TextAlign.Center, lineHeight = 16.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(46.dp)) {
                    Text("Selesai", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun QrInfoRow(icon: ImageVector, text: String, color: Color = TextSecondary) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = color)
    }
}

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val hints  = mapOf(EncodeHintType.MARGIN to 3)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) for (y in 0 until size) {
        bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
    return bitmap
}
