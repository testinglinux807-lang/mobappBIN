package com.example.brin.data.websocket

import com.example.brin.data.api.RetrofitClient
import com.example.brin.data.api.WS_URL
import com.example.brin.data.local.AppState
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

object WebSocketManager {

    private val gson = Gson()
    private var socket: WebSocket? = null

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    fun connect() {
        val token = AppState.token ?: return
        val req = Request.Builder()
            .url("$WS_URL?token=$token")
            .build()
        socket = RetrofitClient.newWebSocketClient().newWebSocket(req, Listener { _events.tryEmit(it) })
    }

    fun disconnect() {
        socket?.close(1000, "User logout")
        socket = null
    }

    private class Listener(private val emit: (WsEvent) -> Unit) : WebSocketListener() {
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
                    else -> return
                }
                emit(wsEvent)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            emit(WsEvent.Error(t.message ?: "WebSocket error"))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            emit(WsEvent.Disconnected)
        }
    }
}

private fun JsonObject.str(key: String)  = get(key)?.asString ?: ""
private fun JsonObject.dbl(key: String)  = get(key)?.asDouble ?: 0.0
private fun JsonObject.int_(key: String) = get(key)?.asInt    ?: 0
