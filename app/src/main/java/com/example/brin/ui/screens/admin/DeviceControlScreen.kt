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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.brin.data.api.DeviceState
import com.example.brin.data.repository.DeviceRepository
import com.example.brin.data.websocket.WsEvent
import com.example.brin.data.websocket.WebSocketManager
import com.example.brin.ui.theme.*
import com.example.brin.util.toUserMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val LOG_TTL_SEC = 600
private const val MAX_LOG = 2000
private const val MAX_LOG_CHARS = 180_000

@Composable
fun DeviceControlScreen(onBack: () -> Unit, nodeId: String) {
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<DeviceState?>(null) }
    var lastUpdate by remember { mutableStateOf<String?>(null) }
    var lastErr by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var logStreamOn by remember { mutableStateOf(false) }

    // ── log panel ────────────────────────────────────────────────────────────
    var logEnabled by remember { mutableStateOf(true) }
    var logLines by remember { mutableStateOf<List<String>>(emptyList()) }
    val logState = rememberLazyListState()
    var logScrollFollow by remember { mutableStateOf(true) }
    var logDropped by remember { mutableStateOf(false) }

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

    // clearErr=false dipakai saat dipanggil setelah perintah gagal — kalau true,
    // fetch yang sukses akan menghapus pesan error perintah sebelum sempat dibaca.
    fun fetchState(clearErr: Boolean = true) {
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
                    if (clearErr) lastErr = null
                }
                .onFailure { lastErr = it.toUserMessage("Gagal memuat status.") }
            loading = false
        }
    }

    // Masuk layar hanya memuat status — tidak menyalakan apa pun di Pi.
    // Log streaming dinyalakan manual lewat Switch, main.py lewat tombol
    // "Nyalakan Smartbin". Keran log tetap MATI supaya tidak membakar kuota
    // HiveMQ saat layar cuma dibuka sekilas.
    LaunchedEffect(Unit) {
        fetchState()
    }

    LaunchedEffect(Unit) {
        WebSocketManager.events.collectLatest { e ->
            when (e) {
                is WsEvent.DeviceState -> {
                    if (e.nodeId == nodeId) {
                        val prev = state
                        state = DeviceState(
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
            fetchState(clearErr = lastErr == null)
            onDone()
        }
    }

    // Action "run" beda dari perintah lain: main.py memanggil os._exit(0) supaya
    // systemd (Restart=always) menghidupkannya kembali. Prosesnya mati SEBELUM
    // sempat mengirim ack, jadi backend selalu kehabisan waktu 12 detik dan balas
    // "Perangkat tidak merespons". Itu hasil yang diharapkan, bukan kegagalan —
    // makanya timeout di sini dihitung sukses. Error lain tetap ditampilkan.
    fun runSmartbin(onDone: (Boolean) -> Unit = {}) {
        lastErr = null
        scope.launch {
            var ok = true
            DeviceRepository.sendCommand(nodeId, "run", null)
                .onSuccess { resp ->
                    val ack = resp.data
                    if (ack != null && !ack.ok) { lastErr = ack.error ?: resp.message; ok = false }
                }
                .onFailure { e ->
                    val msg = e.toUserMessage("Gagal mengirim perintah.")
                    if (!msg.isAckTimeout()) { lastErr = msg; ok = false }
                }
            // Pi butuh beberapa detik buat restart — refresh sekali lagi setelah
            // sempat naik, supaya kartu status tidak nyangkut di kondisi lama.
            fetchState(clearErr = lastErr == null)
            onDone(ok)
            if (ok) {
                kotlinx.coroutines.delay(5000)
                fetchState(clearErr = false)
            }
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
            fetchState(clearErr = lastErr == null)
            onDone()
        }
    }

    fun runCameraStop(onDone: () -> Unit = {}) {
        lastErr = null
        scope.launch {
            DeviceRepository.cameraStop(nodeId)
                .onFailure { lastErr = it.toUserMessage("Gagal mematikan kamera.") }
            fetchState(clearErr = lastErr == null)
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

    Scaffold(topBar = {}) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(AppBackground)) {
            // ── header ────────────────────────────────────────────────────────
            Header(onBack, nodeId)

            // ── konten ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    // navigationBarsPadding() baca tinggi nav bar asli dari sistem —
                    // gesture pill (~24dp) dan 3-tombol (~48dp) dapat jarak yang pas,
                    // beda dengan angka mati yang selalu salah di salah satu model.
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .padding(bottom = 16.dp)
            ) {
                if (lastErr != null) {
                    ErrorBanner(lastErr!!) { lastErr = null }
                }

                StatusCard(state, loading, lastUpdate, onRefresh = { fetchState() }, online = state?.online ?: false)

                Spacer(Modifier.height(16.dp))

                // ── tombol utama: nyalakan / matikan smartbin ─────────────────
                // Label & aksi diturunkan dari state.camera (kondisi asli Pi), bukan
                // flag lokal — jadi kalau dinyalakan dari HP lain, tombol di sini ikut
                // berubah lewat WebSocket tanpa perlu segarkan manual.
                val camRunning = state?.camera == "running"
                var smartbinBusy by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        smartbinBusy = true
                        if (camRunning) runCameraStop { smartbinBusy = false }
                        else runCameraStart { smartbinBusy = false }
                    },
                    enabled = !smartbinBusy && state != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (camRunning) StatusCritical else GreenPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (smartbinBusy) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(if (camRunning) "Mematikan…" else "Menyalakan…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            if (camRunning) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = null, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (camRunning) "Matikan Smartbin" else "Nyalakan Smartbin",
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        state == null -> "Menunggu data perangkat…"
                        camRunning    -> "Smartbin aktif - kamera menyortir sampah otomatis."
                        else          -> "Smartbin siaga - perangkat tetap terhubung, penyortiran berhenti."
                    },
                    color = if (camRunning) StatusNormal else TextSecondary, fontSize = 12.sp
                )

                Spacer(Modifier.height(20.dp))

                // ── daya ──────────────────────────────────────────────────────
                SectionLabel("Daya")
                var powerAction by remember { mutableStateOf<String?>(null) }
                var runBusy by remember { mutableStateOf(false) }
                var runDone by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { powerAction = "reboot" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reboot")
                    }
                    OutlinedButton(
                        onClick = { powerAction = "shutdown" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCritical),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shutdown")
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Restart layanan (action "run"): jalankan ulang main.py tanpa reboot OS.
                // Dipakai kalau layanan nyangkut — bukan tombol nyala/mati sehari-hari.
                OutlinedButton(
                    onClick = { powerAction = "run" },
                    enabled = !runBusy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (runBusy) {
                        CircularProgressIndicator(color = TextSecondary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Merestart…")
                    } else {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restart Layanan")
                    }
                }
                if (runDone) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Layanan direstart. Perangkat kembali terhubung beberapa detik lagi.",
                        color = StatusNormal, fontSize = 12.sp
                    )
                }
                powerAction?.let { action ->
                    PowerDialog(
                        action = action,
                        onCancel = { powerAction = null },
                        onConfirm = {
                            powerAction = null
                            when (action) {
                                "shutdown" -> runCommand("shutdown")
                                "reboot"   -> runCommand("reboot")
                                else       -> { runBusy = true; runDone = false
                                                runSmartbin { ok -> runBusy = false; runDone = ok } }
                            }
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── log panel ─────────────────────────────────────────────────
                LogPanel(
                    logStreamOn = logStreamOn,
                    logEnabled = logEnabled,
                    logLines = logLines,
                    logState = logState,
                    logScrollFollow = logScrollFollow,
                    logDropped = logDropped,
                    onToggle = { on -> logEnabled = on; runToggleLogs(on) },
                    onToggleStream = { runToggleLogs(!logStreamOn) },
                    onToggleFollow = { logScrollFollow = !logScrollFollow },
                )
            }
        }
    }
}

// ── komponen ─────────────────────────────────────────────────────────────────

@Composable
private fun Header(onBack: () -> Unit, nodeId: String) {
    // Header 1 baris tipis — samain ukurannya dengan layar lain (mis. Manajemen Bin).
    // Dot Online/Offline dipindah ke kartu Status Perangkat di bawah.
    Box(modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Text(nodeId, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                "Remote Control",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = StatusCriticalBg,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = StatusCritical, fontSize = 13.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = StatusCritical, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun StatusCard(
    state: DeviceState?,
    loading: Boolean,
    lastUpdate: String?,
    onRefresh: () -> Unit,
    online: Boolean,
) {
    Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(if (online) GreenLight else StatusCritical, RoundedCornerShape(50))
                )
                Spacer(Modifier.width(6.dp))
                Text("Status Perangkat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                Text("${if (online) "Online" else "Offline"} · ${lastUpdate ?: "-"}", fontSize = 11.sp, color = TextHint)
                Spacer(Modifier.width(8.dp))
                if (loading) CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                else TextButton(onClick = onRefresh) { Text("Segarkan", fontSize = 12.sp, color = GreenPrimary) }
            }
            Spacer(Modifier.height(12.dp))

            if (state == null) {
                Text(
                    "Belum ada data dari perangkat. Pastikan main.py berjalan dengan REMOTE_CONTROL=1, lalu coba segarkan.",
                    color = TextSecondary, fontSize = 13.sp
                )
            } else {
                StatusRow(Icons.Default.Videocam, "Kamera",
                    when (state.camera) {
                        "running" -> "● Berjalan"
                        "stopped" -> "● Mati"
                        "error"   -> "✕ Error"
                        else      -> "-"
                    },
                    if (state.camera == "running") StatusNormal else if (state.camera == "error") StatusCritical else TextSecondary)
                if (state.camera == "error" && !state.camera_error.isNullOrBlank()) {
                    Text(state.camera_error, color = StatusCritical, fontSize = 11.sp, modifier = Modifier.padding(start = 34.dp))
                }
                Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                StatusRow(Icons.Default.Settings, "STM32", serialLabel(state.serial_stm32),
                    if (state.serial_stm32 == "connected") StatusNormal else if (state.serial_stm32 == "disconnected") StatusWarning else TextSecondary)
                Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                StatusRow(Icons.Default.SettingsInputAntenna, "LoRa", serialLabel(state.serial_lora),
                    if (state.serial_lora == "connected") StatusNormal else if (state.serial_lora == "disconnected") StatusWarning else TextSecondary)
                Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                lastUpdate?.let { upd ->
                    StatusRow(Icons.Default.Refresh, "Update", upd, TextSecondary)
                    Divider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                }
                StatusRow(Icons.Default.Schedule, "Uptime", state.uptime_sec?.let { formatUptime(it) } ?: "-", TextSecondary)
            }
        }
    }
}

@Composable
private fun PowerDialog(action: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val isShutdown = action == "shutdown"
    val isRun      = action == "run"
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = CardBg,
        title = {
            Text(
                when {
                    isShutdown -> "Matikan Perangkat?"
                    isRun      -> "Restart Layanan?"
                    else       -> "Restart Perangkat?"
                },
                color = TextPrimary, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                when {
                    isShutdown -> "Raspi akan mati total. Kamu harus SSH ke raspi untuk nyalain lagi. Lanjut?"
                    isRun      -> "Layanan smartbin dijalankan ulang tanpa mematikan raspi. Terputus beberapa detik. Lanjut?"
                    else       -> "Raspi akan restart. Butuh beberapa detik sampai online lagi. Lanjut?"
                },
                color = TextSecondary, fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (isShutdown) StatusCritical else GreenPrimary)
            ) { Text(if (isShutdown) "Matikan" else "Restart") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Batal", color = TextSecondary) }
        }
    )
}

@Composable
private fun LogPanel(
    logStreamOn: Boolean,
    logEnabled: Boolean,
    logLines: List<String>,
    logState: androidx.compose.foundation.lazy.LazyListState,
    logScrollFollow: Boolean,
    logDropped: Boolean,
    onToggle: (Boolean) -> Unit,
    onToggleStream: () -> Unit,
    onToggleFollow: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Log Perangkat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = logStreamOn, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GreenPrimary)
        )
        Text(if (logStreamOn) "Hidup" else "Mati", fontSize = 12.sp, color = TextSecondary)
    }

    if (logStreamOn) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Log aktif (TTL $LOG_TTL_SEC s)", fontSize = 12.sp, color = StatusNormal)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToggleStream) {
                Text(if (logStreamOn) "Matikan Streaming" else "Nyalakan Streaming", fontSize = 12.sp, color = GreenPrimary)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onToggleFollow) {
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

// ── helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun StatusRow(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(90.dp))
        Text(value, fontSize = 13.sp, color = tint, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

/** Status serial dari Pi ("connected"/"disconnected") jadi label Indonesia. */
private fun serialLabel(v: String?): String = when (v) {
    "connected"    -> "Terhubung"
    "disconnected" -> "Terputus"
    null           -> "-"
    else           -> v
}

/**
 * Backend balas "Perangkat {nodeId} tidak merespons dalam 12s; perintah mungkin
 * tetap dieksekusi" saat ack tidak datang. Untuk action "run" itu memang selalu
 * terjadi (proses bunuh diri sebelum sempat balas), jadi dikenali di sini.
 */
private fun String.isAckTimeout(): Boolean =
    contains("tidak merespons", ignoreCase = true) || contains("tidak merespon", ignoreCase = true)

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
