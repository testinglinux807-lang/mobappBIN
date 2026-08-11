package com.example.brin.data.websocket

import android.util.Log
import com.example.brin.data.api.RetrofitClient
import com.example.brin.data.api.WS_URL
import com.example.brin.data.local.AppState
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val TAG = "WSManager"

object WebSocketManager {

    private val gson = Gson()
    private var socket: WebSocket? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var manualClose = false
    private var reconnectJob: Job? = null
    private var attempt = 0

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    fun connect() {
        manualClose = false
        reconnectJob?.cancel()
        openSocket()
    }

    private fun openSocket() {
        val token = AppState.token
        if (token == null) { Log.w(TAG, "connect aborted: not authenticated"); return }
        Log.d(TAG, "connecting to WebSocket")
        // Send the JWT in the Authorization header (not the URL) so it can't leak
        // into proxy/access logs, browser history, or referrers.
        val req = Request.Builder()
            .url(WS_URL)
            .header("Authorization", "Bearer $token")
            .build()
        socket = RetrofitClient.newWebSocketClient().newWebSocket(
            req,
            Listener(
                emit         = { _events.tryEmit(it) },
                onConnected  = { attempt = 0; _connected.value = true },
                onDropped    = { _connected.value = false; scheduleReconnect() }
            )
        )
    }

    fun disconnect() {
        manualClose = true
        reconnectJob?.cancel()
        socket?.close(1000, "User logout")
        socket = null
        _connected.value = false
    }

    private fun scheduleReconnect() {
        if (manualClose || AppState.token == null) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            attempt++
            // exponential backoff: 1s, 2s, 4s, 8s, 16s, capped at 30s
            val delayMs = minOf(30_000L, 1_000L * (1L shl minOf(attempt, 5)))
            delay(delayMs)
            if (!manualClose) openSocket()
        }
    }

    private class Listener(
        private val emit: (WsEvent) -> Unit,
        private val onConnected: () -> Unit,
        private val onDropped: () -> Unit
    ) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "onOpen: connected (HTTP ${response.code})")
            onConnected()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching {
                val obj    = gson.fromJson(text, JsonObject::class.java)
                val event  = obj.get("event")?.asString ?: return
                val payload = obj.get("payload")?.asJsonObject ?: JsonObject()
                val wsEvent: WsEvent = when (event) {
                    "CONNECTED" -> WsEvent.Connected
                    "BIN_UPDATE" -> WsEvent.BinUpdate(
                        nodeId    = payload.str("nodeId"),
                        binId     = payload.str("binId"),
                        weight    = payload.dbl("weight"),
                        volume    = payload.int_("volume"),
                        battery   = payload.int_("battery"),
                        gas       = payload.dbl("gas"),
                        rssi      = payload.int_("rssi"),
                        timestamp = payload.str("timestamp")
                    )
                    "BIN_STATUS" -> WsEvent.BinStatus(
                        nodeId   = payload.str("nodeId"),
                        status   = payload.str("status"),
                        lastSeen = payload.get("lastSeen")?.takeUnless { it.isJsonNull }?.asString
                    )
                    "ALERT_NEW" -> WsEvent.AlertNew(
                        alertId   = payload.str("alertId"),
                        nodeId    = payload.str("nodeId"),
                        binId     = payload.str("binId"),
                        type      = payload.str("type"),
                        message   = payload.str("message"),
                        createdAt = payload.str("createdAt"),
                        areaId    = payload.get("areaId")?.takeUnless { it.isJsonNull }?.asString
                    )
                    "ALERT_RESOLVED" -> WsEvent.AlertResolved(
                        alertId = payload.str("alertId"),
                        nodeId  = payload.str("nodeId"),
                        binId   = payload.str("binId"),
                        type    = payload.str("type")
                    )
                    "CLASSIFICATION_NEW" -> WsEvent.ClassificationNew(
                        id         = payload.str("id"),
                        nodeId     = payload.str("nodeId"),
                        binId      = payload.str("binId"),
                        label      = payload.str("label"),
                        confidence = payload.dbl("confidence"),
                        createdAt  = payload.str("createdAt")
                    )
                    "DEVICE_STATE" -> WsEvent.DeviceState(
                        nodeId      = payload.str("nodeId"),
                        online      = payload.bool("online"),
                        camera      = payload.nullableStr("camera"),
                        cameraError = payload.nullableStr("camera_error"),
                        lastDetection = payload.get("last_detection")
                            ?.takeUnless { it.isJsonNull }
                            ?.asJsonObject
                            ?.let { d ->
                                val kat  = d.get("kategori")?.takeUnless { it.isJsonNull }?.asString
                                val conf = d.get("confidence")?.takeUnless { it.isJsonNull }?.asDouble
                                when {
                                    kat.isNullOrBlank() -> null
                                    conf == null        -> kat
                                    else                -> "$kat ${(conf * 100).toInt()}%"
                                }
                            },
                        serialStm32 = payload.nullableStr("serial_stm32"),
                        serialLora  = payload.nullableStr("serial_lora"),
                        sensorData  = payload.nullableStr("sensor_data"),
                        uptimeSec   = payload.nullableDbl("uptime_sec"),
                        logStream   = payload.bool("log_stream"),
                        reason      = payload.nullableStr("reason")
                    )
                    "DEVICE_ACK" -> WsEvent.DeviceAck(
                        nodeId    = payload.str("nodeId"),
                        ok        = payload.bool("ok"),
                        action    = payload.nullableStr("action"),
                        error     = payload.nullableStr("error"),
                        duplicate = payload.get("duplicate")?.takeUnless { it.isJsonNull }?.asBoolean
                    )
                    "DEVICE_LOG" -> WsEvent.DeviceLog(
                        nodeId  = payload.str("nodeId"),
                        lines   = payload.get("lines")
                            ?.takeUnless { it.isJsonNull }
                            ?.asJsonArray
                            ?.mapNotNull { it.takeUnless { n -> n.isJsonNull }?.asString }
                            ?: emptyList(),
                        dropped = payload.get("dropped")?.takeUnless { it.isJsonNull }?.asInt
                    )
                    else -> return
                }
                emit(wsEvent)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "onFailure: ${t.message} (HTTP ${response?.code}) body=${runCatching { response?.body?.string() }.getOrNull()}", t)
            emit(WsEvent.Error(t.message ?: "WebSocket error"))
            onDropped()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "onClosing: $code $reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "onClosed: $code $reason")
            emit(WsEvent.Disconnected)
            onDropped()
        }
    }
}

private fun JsonObject.str(key: String)  = get(key)?.asString ?: ""
private fun JsonObject.dbl(key: String)  = get(key)?.asDouble ?: 0.0
private fun JsonObject.int_(key: String) = get(key)?.asInt    ?: 0
private fun JsonObject.nullableStr(key: String): String? =
    get(key)?.takeUnless { it.isJsonNull }?.asString
private fun JsonObject.nullableDbl(key: String): Double? =
    get(key)?.takeUnless { it.isJsonNull }?.asDouble
private fun JsonObject.bool(key: String): Boolean =
    get(key)?.takeUnless { it.isJsonNull }?.asBoolean ?: false
