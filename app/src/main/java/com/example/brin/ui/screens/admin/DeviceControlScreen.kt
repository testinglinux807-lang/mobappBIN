package com.example.brin.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.api.DeviceCommandResponse
import com.example.brin.data.repository.DeviceRepository
import com.example.brin.data.websocket.WsEvent
import com.example.brin.data.websocket.WebSocketManager
import com.example.brin.ui.theme.*
import com.example.brin.util.toUserMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val LOG_TTL_SEC = 600

@Composable
fun DeviceControlScreen(onBack: () -> Unit, nodeId: String) {
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<com.example.brin.data.api.DeviceState?>(null) }
    var lastUpdate by remember { mutableStateOf<String?>(null) }
    var lastErr by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var logStreamOn by remember { mutableStateOf(false) }

    // ── log panel ─────────────────────────────────────────────────────────────
    var logEnabled by remember { mutableStateOf(true) }
    var logLines by remember { mutableStateOf<List<String>>(emptyList()) }
    val logState = rememberLazyListState()
    var logScrollFollow by remember { mutableStateOf(true) }
    var logDropped by remember { mutableStateOf(false) }

    val MAX_LOG = 2000
    val MAX_LOG_CHARS = 180_000

    fun appendLog(new: List<String>) {
        if (new.isEmpty()) return
        val keep = logLines.takeLast(MAX_LOG - new.size).plus(new)
        val joined = keep.joinToString("\n")
        if (joined.length > MAX_LOG_CHARS) {
            // buang baris tertua sampai muat — pangkas per baris, bukan potong tengah string
            val buf = joined.takeLast(MAX_LOG_CHARS)
            val cut = buf.indexOf('\n')
            logLines = if (cut >= 0) buf.substring(cut + 1).split('\n') else buf.split('\n')
        } else {
            logLines = keep
        }
        logDropped = false
    }

    fun fetchState() {
        loading = true
        scope.launch {
            DeviceRepository.getDeviceStates()
                .onSuccess { all ->
                    val d = all[nodeId]
                    if (d != null) {
                        state = d
                        lastUpdate = formatTs(d.ts)
                        logStreamOn = d.log_stream
                    }
                }
                .onFailure { lastErr = it.toUserMessage("Gagal memuat status.") }
            loading = false
        }
    }

    LaunchedEffect(Unit) { fetchState() }

    // Log streaming: begitu masuk layar, pastikan keran nyala. Kalau akun di-set
    // fresh, ttl-nya abis; toggle tombol log di bawah buat nyalain lagi.
    LaunchedEffect(Unit) {
        WebSocketManager.events.collectLatest { e ->
            when (e) {
                is WsEvent.DeviceState -> {
                    if (e.nodeId == nodeId) {
                        val prev = state
                        state = com.example.brin.data.api.DeviceState(
                            nodeId = e.nodeId, online = e.online, camera = e.camera,
                            camera_error = e.cameraError, last_detection = prev?.last_detection,
                            serial_stm32 = e.serialStm32, serial_lora = e.serialLora,
                            sensor_data = e.sensorData, uptime_sec = e.uptimeSec,
                            log_stream = e.logStream, reason = e.reason
                        )
                        lastUpdate = "Baru saja"
                    }
                }
                is WsEvent.DeviceAck -> {
                    if (e.nodeId == nodeId && !e.ok) {
                        lastErr = e.error ?: "Perintah ditolak perangkat."
                    }
                }
                is WsEvent.DeviceLog -> {
                    if (e.nodeId == nodeId && logEnabled) {
                        appendLog(e.lines)
                        if (e.dropped != null && e.dropped > 0) logDropped = true
                    }
                }
                else -> {}
            }
        }
    }

    // Auto-scroll ikut baris log terbaru saat tombol "ikuti" aktif.
    LaunchedEffect(logLines.size) {
        if (logScrollFollow && logLines.isNotEmpty()) {
            logState.animateScrollToItem(logLines.size - 1)
        }
    }

    // Jalankan perintah generik, tampilkan error kalau gagal, refresh state.
    fun runCommand(action: String, args: Map<String, Any?>? = null, onDone: () -> Unit = {}) {
        lastErr = null
        scope.launch {
            DeviceRepository.sendCommand(nodeId, action, args)
                .onSuccess { resp ->
                    val ack = resp.data
                    if (ack == null) lastErr = resp.message
                    else if (!ack.ok) lastErr = ack.error ?: resp.message
                }
                .onFailure { lastErr = it.toUserMessage("Gagal mengirim perintah.") }
            fetchState()
            onDone()
        }
    }

    fun runCameraStart(onDone: () -> Unit = {}) {
        lastErr = null
        scope.launch {
            DeviceRepository.cameraStart(nodeId)
                .onSuccess { resp ->
                    val ack = resp.data
                    if (ack == null) lastErr = resp.message
                    else if (!ack.ok) lastErr = ack.error ?: resp.message
                }
                .onFailure { lastErr = it.toUserMessage("Gagal menyalakan kamera.") }
            fetchState()
            onDone()
        }
    }

    fun runCameraStop(onDone: () -> Unit = {}) {
        lastErr = null
        scope.launch {
            DeviceRepository.cameraStop(nodeId)
                .onFailure { lastErr = it.toUserMessage("Gagal mematikan kamera.") }
            fetchState()
            onDone()
        }
    }

    fun runToggleLogs(on: Boolean) {
        lastErr = null
        scope.launch {
            DeviceRepository.toggleLogs(nodeId, on, if (on) LOG_TTL_SEC else null)
                .onSuccess { logStreamOn = on }
                .onFailure { lastErr = it.toUserMessage("Gagal mengubah log stream.") }
        }
    }

    Scaffold(topBar = {}) { _ ->
        Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
            // ── header ─────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(nodeId, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Remote Control", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                    // indikator online/offline
                    StatusDot(state?.online ?: false)
                    Spacer(Modifier.width(16.dp))
                }
            }

            // ── konten ─────────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 16.dp)) {
                if (lastErr != null) {
                    Surface(shape = RoundedCornerShape(10.dp), color = StatusCriticalBg, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(lastErr!!, color = StatusCritical, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // ── kartu status ────────────────────────────────────────────────
                Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Status Perangkat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                            if (loading) CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            else TextButton(onClick = { fetchState() }) { Text("Segarkan", fontSize = 12.sp, color = GreenPrimary) }
                        }
                        Spacer(Modifier.height(12.dp))

                        val s = state
                        if (s == null) {
                            Text(
                                "Belum ada data dari perangkat. Pastikan main.py berjalan dengan REMOTE_CONTROL=1, lalu coba segarkan.",
                                color = TextSecondary, fontSize = 13.sp
                            )
                        } else {
                            StatusRow(Icons.Default.Science, "Kamera",
                                when (s.camera) {
                                    "running" -> "● Berjalan"
                                    "stopped" -> "● Mati"
                                    "error"   -> "✕ Error"
                                    else      -> "—"
                                },
                                if (s.camera == "running") StatusNormal else if (s.camera == "error") StatusCritical else TextSecondary)
                            if (s.camera == "error" && !s.camera_error.isNullOrBlank()) {
                                Text(s.camera_error, color = StatusCritical, fontSize = 11.sp, modifier = Modifier.padding(start = 34.dp))
                            }
                            Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                            StatusRow(Icons.Default.Sensors, "Sensor", s.sensor_data ?: "—", if (s.sensor_data == "ada") StatusNormal else TextSecondary)
                            Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                            StatusRow(Icons.Default.Settings, "STM32", s.serial_stm32 ?: "—",
                                if (s.serial_stm32 == "connected") StatusNormal else if (s.serial_stm32 == "disconnected") StatusWarning else TextSecondary)
                            Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                            StatusRow(Icons.Default.Settings, "LoRa", s.serial_lora ?: "—",
                                if (s.serial_lora == "connected") StatusNormal else if (s.serial_lora == "disconnected") StatusWarning else TextSecondary)
                            Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                            StatusRow(Icons.Default.Thermostat, "Deteksi", s.last_detection?.let { "${it.kategori ?: ""}${it.confidence?.let { c -> " ${(c * 100).toInt()}%" } ?: ""}" } ?: "Belum ada",
                                if (s.last_detection != null) StatusNormal else TextSecondary)
                            Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                            lastUpdate?.let { upd ->
                                StatusRow(Icons.Default.Refresh, "Update", upd, TextSecondary)
                                Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            StatusRow(Icons.Default.RssFeed, "Uptime", s.uptime_sec?.let { formatUptime(it) } ?: "—", TextSecondary)
                        }
                    }
                }

                // ── tombol aksi ─────────────────────────────────────────────────
                Spacer(Modifier.height(16.dp))
                var camOnBusy by remember { mutableStateOf(false) }
                var camOffBusy by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { camOnBusy = true; runCameraStart { camOnBusy = false } },
                        enabled = !camOnBusy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Kamera Nyala") }
                    OutlinedButton(
                        onClick = { camOffBusy = true; runCameraStop { camOffBusy = false } },
                        enabled = !camOffBusy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCritical),
                        shape = RoundedCornerShape(12.dp)
                    ) { Icon(Icons.Default.VideocamOff, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Kamera Mati") }
                }

                Spacer(Modifier.height(12.dp))
                // shutdown/reboot raspi — butuh konfirmasi karena matiin device
                var powerAction by remember { mutableStateOf<String?>(null) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { powerAction = "reboot" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Reboot") }
                    OutlinedButton(
                        onClick = { powerAction = "shutdown" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCritical),
                        shape = RoundedCornerShape(12.dp)
                    ) { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Shutdown") }
                }
                powerAction?.let { action ->
                    val isShutdown = action == "shutdown"
                    AlertDialog(
                        onDismissRequest = { powerAction = null },
                        containerColor   = CardBg,
                        title = { Text(if (isShutdown) "Matikan Perangkat?" else "Restart Perangkat?", color = TextPrimary, fontWeight = FontWeight.Bold) },
                        text  = {
                            Text(
                                if (isShutdown) "Raspi akan mati total. Kamu harus SSH ke raspi untuk nyalain lagi. Lanjut?"
                                else "Raspi akan restart. Butuh beberapa detik sampai online lagi. Lanjut?",
                                color = TextSecondary, fontSize = 14.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    powerAction = null
                                    if (isShutdown) runCommand("shutdown")
                                    else runCommand("reboot")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isShutdown) StatusCritical else GreenPrimary)
                            ) { Text(if (isShutdown) "Matikan" else "Restart") }
                        },
                        dismissButton = {
                            TextButton(onClick = { powerAction = null }) { Text("Batal", color = TextSecondary) }
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
                // Nyalakan Smartbin — jalankan ulang main.py di Pi kalau belum jalan.
                var runBusy by remember { mutableStateOf(false) }
                var runDone by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        runBusy = true; runDone = false
                        runCommand("run", args = null) { runBusy = false; runDone = true }
                    },
                    enabled = !runBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (runBusy) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp)); Text("Menjalankan…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text("Nyalakan Smartbin", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (runDone) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Perintah dikirim. main.py otomatis jalan kembali (kamera nyala). Log di bawah akan berisi baris startup baru.",
                        color = StatusNormal, fontSize = 12.sp
                    )
                }

                // ── log panel ───────────────────────────────────────────────────
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Log Perangkat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = logEnabled, onCheckedChange = { logEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GreenPrimary)
                    )
                    Text(if (logEnabled) "Hidup" else "Mati", fontSize = 12.sp, color = TextSecondary)
                }

                if (logEnabled) {
                    // tombol log-stream + ikuti auto-scroll
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (logStreamOn) {
                            Text("Log aktif (TTL $LOG_TTL_SEC s)", fontSize = 12.sp, color = StatusNormal)
                        }
                        TextButton(onClick = { runToggleLogs(!logStreamOn) }) {
                            Text(if (logStreamOn) "Matikan Streaming" else "Nyalakan Streaming", fontSize = 12.sp, color = GreenPrimary)
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { logScrollFollow = !logScrollFollow }) {
                            Text(if (logScrollFollow) "Mengikuti" else "Jeda", fontSize = 12.sp, color = GreenPrimary)
                        }
                    }
                    if (logDropped) {
                        Text("Beberapa baris log terbuang (koneksi lambat).", color = StatusWarning, fontSize = 11.sp)
                    }

                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF0D1117), modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        if (logLines.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Menunggu log… nyalakan streaming di atas.", color = Color(0xFF8B949E), fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(state = logState, modifier = Modifier.fillMaxSize()) {
                                items(count = logLines.size) { i ->
                                    Text(
                                        logLines[i],
                                        color = Color(0xFFC9D1D9),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun StatusDot(online: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(if (online) StatusNormal else StatusCritical, RoundedCornerShape(50))
    )
}

@Composable
private fun StatusRow(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(90.dp))
        Text(value, fontSize = 13.sp, color = tint, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

private fun formatUptime(sec: Double): String {
    val s = sec.toLong()
    val h = s / 3600
    val m = (s % 3600) / 60
    val d = s / 86400
    return if (d > 0) "${d} hari ${h % 24} jam" else if (h > 0) "${h} jam ${m} mnt" else "${m} mnt ${s % 60} dtk"
}

private fun formatTs(ts: Double?): String? {
    if (ts == null) return null
    val millis = (ts * 1000).toLong()
    val diff = (System.currentTimeMillis() - millis) / 1000
    return when {
        diff < 60 -> "Baru saja"
        diff < 3600 -> "${diff / 60} menit lalu"
        diff < 86400 -> "${diff / 3600} jam lalu"
        else -> "${diff / 86400} hari lalu"
    }
}

