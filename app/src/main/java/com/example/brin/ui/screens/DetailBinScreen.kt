package com.example.brin.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.brin.data.BinData
import com.example.brin.data.BinStatus
import com.example.brin.data.MockData
import com.example.brin.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun DetailBinScreen(binId: String, onBack: () -> Unit) {
    val bin = MockData.getBinById(binId)
    var showQrDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Surface(color = CardBg) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                }
                Text(
                    "Detail Bin",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondary)
                }
            }
        }

        if (bin == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bin tidak ditemukan", color = TextHint)
            }
            return
        }

        val sColor = statusColor(bin.status)
        val sLabel = statusLabel(bin.status)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Bin identity card
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(sColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (bin.status == BinStatus.CRITICAL)
                                        Icons.Default.Warning else Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = sColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(bin.id, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(bin.location, fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = sColor.copy(alpha = 0.12f)) {
                            Text(
                                sLabel,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color      = sColor,
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (bin.alertText.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = sColor.copy(alpha = 0.08f)) {
                            Row(
                                modifier          = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null,
                                    tint = sColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Kapasitas ${bin.capacity}% · ${bin.alertText}",
                                    fontSize = 12.sp, color = sColor, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Capacity bar
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kapasitas", fontSize = 14.sp, color = TextSecondary)
                        Text("${bin.capacity}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sColor)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress   = { bin.capacity / 100f },
                        modifier   = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color      = sColor,
                        trackColor = DividerColor
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Informasi Bin
            InfoSection(bin)

            Spacer(Modifier.height(16.dp))

            // Aksi Cepat
            Text(
                "Aksi Cepat",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                modifier   = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))

            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = CardBg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column {
                    QuickAction(Icons.Default.QrCode2,       "Generate QR Petugas", GreenPrimary, filled = true) { showQrDialog = true }
                    HorizontalDivider(color = DividerColor)
                    QuickAction(Icons.Default.CheckCircle,   "Pickup Selesai",      TextSecondary) { }
                    HorizontalDivider(color = DividerColor)
                    QuickAction(Icons.Default.Warning,       "Laporkan Masalah",    TextSecondary) { }
                    HorizontalDivider(color = DividerColor)
                    QuickAction(Icons.Default.Navigation,    "Navigasi ke Lokasi",  TextSecondary) { }
                }
            }
        }
    }

    if (showQrDialog && bin != null) {
        QrDialog(bin = bin, onDismiss = { showQrDialog = false })
    }
}

@Composable
private fun InfoSection(bin: BinData) {
    Text(
        "Informasi Bin",
        fontSize   = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextPrimary,
        modifier   = Modifier.padding(horizontal = 20.dp)
    )
    Spacer(Modifier.height(10.dp))
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = CardBg,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            InfoRow(Icons.Default.LocationOn, "Lokasi",          "${bin.location}, ${bin.area}")
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 12.dp))
            InfoRow(Icons.Default.Schedule,   "Terakhir Update", bin.lastUpdate)
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 12.dp))
            InfoRow(Icons.Default.Warning,    "Status",          statusLabel(bin.status))
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 12.dp))
            InfoRow(Icons.Default.BatteryFull,"Baterai",         "${bin.battery}%")
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 12.dp))
            InfoRow(Icons.Default.Thermostat, "Suhu",            "${bin.temperature}°C")
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QrDialog(bin: BinData, onDismiss: () -> Unit) {
    val qrContent = "brin://navigate?binId=${bin.id}&lat=${bin.lat}&lng=${bin.lng}&loc=${bin.location}"
    val qrBitmap  = remember(qrContent) { generateQrBitmap(qrContent) }
    val sColor    = statusColor(bin.status)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardBg
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("QR Code Petugas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(bin.id, fontSize = 13.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextHint)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // QR code image
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Image(
                        bitmap      = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code ${bin.id}",
                        modifier    = Modifier.size(200.dp).padding(8.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Info
                Surface(shape = RoundedCornerShape(10.dp), color = AppBackground) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        QrInfoRow(Icons.Default.LocationOn, bin.location)
                        QrInfoRow(Icons.Default.MyLocation, "${bin.lat}, ${bin.lng}")
                        QrInfoRow(Icons.Default.Info, statusLabel(bin.status), color = sColor)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Tunjukkan QR ini ke petugas. Setelah scan,\npetugas akan diarahkan ke lokasi bin.",
                    fontSize  = 11.sp,
                    color     = TextHint,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick  = onDismiss,
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Selesai", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun QrInfoRow(icon: ImageVector, text: String, color: Color = TextSecondary) {
    Row(
        modifier          = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = color)
    }
}

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val hints    = mapOf(EncodeHintType.MARGIN to 1)
    val matrix   = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap   = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    color: Color,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (filled) GreenPrimary else SearchBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null,
                tint = if (filled) Color.White else color,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            color      = if (filled) GreenPrimary else TextPrimary,
            modifier   = Modifier.weight(1f)
        )
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = TextHint, modifier = Modifier.size(18.dp))
    }
}
