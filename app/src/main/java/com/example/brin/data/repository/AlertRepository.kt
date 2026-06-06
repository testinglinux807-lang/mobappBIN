package com.example.brin.data.repository

import com.example.brin.data.api.ApiAlert
import com.example.brin.data.api.RetrofitClient
import com.example.brin.data.BinStatus
import com.example.brin.data.NotificationItem

object AlertRepository {

    suspend fun getAlerts(resolved: Boolean? = null): Result<List<NotificationItem>> = runCatching {
        val resp = RetrofitClient.api.getAlerts(resolved = resolved)
        if (!resp.success) error("Gagal memuat alert")
        resp.data.orEmpty().map { it.toNotificationItem() }
    }

    suspend fun getUnresolvedCount(): Int = runCatching {
        val resp = RetrofitClient.api.getAlerts(resolved = false, limit = 1)
        resp.data?.size ?: 0
    }.getOrDefault(0)

    suspend fun resolveAlert(id: String): Result<Unit> = runCatching {
        val resp = RetrofitClient.api.resolveAlert(id)
        if (!resp.success) error("Gagal resolve alert")
    }

    /** Build a NotificationItem from a live WebSocket ALERT_NEW payload (no network call). */
    fun liveAlertItem(
        alertId: String, nodeId: String, binId: String,
        type: String, message: String, createdAt: String
    ) = NotificationItem(
        id       = alertId,
        binId    = nodeId.ifBlank { binId },
        title    = type.toTitle(),
        message  = message,
        time     = createdAt.toRelativeTime(),
        type     = type.toBinStatus(),
        isRead   = false,
        binRefId = binId,
        rawType  = type,
        createdAtRaw = createdAt
    )
}

private fun ApiAlert.toNotificationItem() = NotificationItem(
    id       = id,
    binId    = bin?.nodeId ?: binId,
    title    = type.toTitle(),
    message  = message,
    time     = createdAt.toRelativeTime(),
    type     = type.toBinStatus(),
    isRead   = resolved,
    binRefId = binId,
    rawType  = type,
    createdAtRaw = createdAt
)

private fun String.toTitle() = when (this) {
    "FULL_WEIGHT" -> "Bin Penuh (Berat)"
    "FULL_VOLUME" -> "Bin Penuh (Volume)"
    "BATTERY_LOW" -> "Baterai Lemah"
    "GAS_HIGH"   -> "Gas Berbahaya"
    else          -> this
}

private fun String.toBinStatus() = when (this) {
    "FULL_WEIGHT", "FULL_VOLUME", "GAS_HIGH" -> BinStatus.CRITICAL
    "BATTERY_LOW"                             -> BinStatus.NEED_PICKUP
    else                                      -> BinStatus.NORMAL
}

private fun String.toRelativeTime(): String = runCatching {
    val instant = java.time.Instant.parse(this)
    val diff    = java.time.Instant.now().epochSecond - instant.epochSecond
    when {
        diff < 60    -> "Baru saja"
        diff < 3600  -> "${diff / 60} menit lalu"
        diff < 86400 -> "${diff / 3600} jam lalu"
        else         -> "${diff / 86400} hari lalu"
    }
}.getOrDefault(this)
