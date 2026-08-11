package com.example.brin.data.repository

import com.example.brin.data.api.DeviceCommandRequest
import com.example.brin.data.api.DeviceCommandResponse
import com.example.brin.data.api.DeviceLogsRequest
import com.example.brin.data.api.DeviceState
import com.example.brin.data.api.RetrofitClient

/**
 * Remote control Raspberry Pi (bin-003) lewat backend smartbin.sbs.
 *
 * Alur: app → POST /devices/{nodeId}/... → backend publish MQTT device/cmd →
 * Pi (main.py + remote_control.py) jalankan → balas device/ack → backend resolve
 * → response HTTP balik ke app. Biasanya < 1 detik, timeout 12 detik.
 *
 * Log streaming beda jalur: nyalakan keran via /logs, lalu barisnya MASUK lewat
 * WebSocket sebagai event DEVICE_LOG (lihat WebSocketManager), bukan lewat response.
 */
object DeviceRepository {

    /** State semua node yang pernah lapor (retained). Kosong kalau Pi belum pernah online. */
    suspend fun getDeviceStates(): Result<Map<String, DeviceState>> = runCatching {
        val resp = RetrofitClient.api.getDeviceStates()
        if (!resp.success) error(resp.message)
        resp.data.orEmpty()
    }

    /** Tanya langsung ke Pi (live=1). Timeout 12s kalau Pi offline. */
    suspend fun getLiveStatus(nodeId: String): Result<DeviceState> = runCatching {
        val resp = RetrofitClient.api.getDeviceStatus(nodeId, live = 1)
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data
    }

    /** Generic command; args null untuk action tanpa argumen. */
    suspend fun sendCommand(nodeId: String, action: String, args: Map<String, Any?>? = null): Result<DeviceCommandResponse> =
        runCatching {
            val resp = RetrofitClient.api.sendDeviceCommand(
                nodeId, DeviceCommandRequest(action, args)
            )
            if (!resp.success) error(resp.message)
            resp
        }

    suspend fun cameraStart(nodeId: String): Result<DeviceCommandResponse> = runCatching {
        val resp = RetrofitClient.api.deviceCameraStart(nodeId)
        if (!resp.success) error(resp.message)
        resp
    }

    suspend fun cameraStop(nodeId: String): Result<DeviceCommandResponse> = runCatching {
        val resp = RetrofitClient.api.deviceCameraStop(nodeId)
        if (!resp.success) error(resp.message)
        resp
    }

    /** Nyalakan/matikan keran log. Barisnya datang via WS, bukan response ini. */
    suspend fun toggleLogs(nodeId: String, on: Boolean, ttlSec: Int? = null): Result<DeviceCommandResponse> =
        runCatching {
            val resp = RetrofitClient.api.deviceToggleLogs(nodeId, DeviceLogsRequest(on, ttlSec))
            if (!resp.success) error(resp.message)
            resp
        }

    /** Matikan raspi total. Di Pi: sudo poweroff. */
    suspend fun shutdown(nodeId: String): Result<DeviceCommandResponse> =
        sendCommand(nodeId, "shutdown")

    /** Restart raspi. Di Pi: sudo reboot. */
    suspend fun reboot(nodeId: String): Result<DeviceCommandResponse> =
        sendCommand(nodeId, "reboot")
}
