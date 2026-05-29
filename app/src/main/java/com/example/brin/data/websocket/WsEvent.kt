package com.example.brin.data.websocket

sealed class WsEvent {
    data class BinUpdate(
        val nodeId: String, val binId: String,
        val weight: Double, val volume: Int,
        val battery: Int, val gas: Double,
        val rssi: Int, val timestamp: String
    ) : WsEvent()

    data class BinStatus(val nodeId: String, val status: String, val lastSeen: String?) : WsEvent()

    data class AlertNew(
        val alertId: String, val nodeId: String, val binId: String,
        val type: String, val message: String,
        val createdAt: String, val areaId: String?
    ) : WsEvent()

    data class AlertResolved(
        val alertId: String, val nodeId: String, val binId: String, val type: String
    ) : WsEvent()

    data class ClassificationNew(
        val id: String, val nodeId: String, val binId: String,
        val label: String, val confidence: Double, val createdAt: String
    ) : WsEvent()

    object Connected : WsEvent()
    data class Error(val message: String) : WsEvent()
    object Disconnected : WsEvent()
}
