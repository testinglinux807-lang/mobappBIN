package com.example.brin.data.repository

import com.example.brin.data.api.ApiBin
import com.example.brin.data.api.ApiBinDetail
import com.example.brin.data.api.BinLatest
import com.example.brin.data.api.CreateBinRequest
import com.example.brin.data.api.OptimalRouteData
import com.example.brin.data.api.RetrofitClient
import com.example.brin.data.api.ThresholdRequest
import com.example.brin.data.api.UpdateBinRequest
import com.example.brin.data.BinData
import com.example.brin.data.BinStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object BinRepository {

    suspend fun getBins(): Result<List<BinData>> = runCatching {
        val resp = RetrofitClient.api.getBins()
        if (!resp.success) error(resp.message)
        // BE balikin latest = null untuk bin stale/offline → gas/volume tampil 0 di
        // dashboard. Ambil pembacaan terakhir dari history (paralel) sebagai fallback.
        coroutineScope {
            resp.data.orEmpty().map { apiBin ->
                async { apiBin.toBinData().withHistoryFallback(apiBin.latest == null, apiBin.id) }
            }.awaitAll()
        }
    }

    suspend fun getBinById(id: String): Result<BinData> = runCatching {
        val resp = RetrofitClient.api.getBinById(id)
        if (!resp.success || resp.data == null) error(resp.message)
        // BE mengembalikan latest = null saat bin offline/stale, jadi gas/volume
        // tampil 0. Ambil pembacaan terakhir dari history supaya nilai terakhir
        // yang diketahui tetap tampil (bukan 0 yang menyesatkan).
        resp.data.toBinData().withHistoryFallback(resp.data.latest == null, id)
    }

    // Isi gas/volume/battery dari pembacaan history terakhir bila BE mengembalikan
    // latest = null (bin stale/offline). Aman gagal — kalau history kosong/error,
    // BinData asli (nilai 0) tetap dikembalikan.
    private suspend fun BinData.withHistoryFallback(needsFallback: Boolean, id: String): BinData {
        if (!needsFallback) return this
        val log = runCatching {
            RetrofitClient.api.getBinHistory(id, page = 1, limit = 1).data?.firstOrNull()
        }.getOrNull() ?: return this
        return copy(
            capacity   = log.volume,
            gas        = log.gas,
            battery    = log.battery,
            lastUpdate = log.createdAt.toRelativeTime(),
            status     = log.volume.toStatus()
        )
    }

    suspend fun getOptimalRoute(lat: Double, lng: Double): Result<OptimalRouteData> = runCatching {
        val resp = RetrofitClient.api.getOptimalRoute(lat, lng)
        if (!resp.success) error(resp.message)
        resp.data ?: OptimalRouteData(emptyList(), null, null, resp.message)
    }

    suspend fun createBin(
        nodeId: String, location: String, lat: Double, lng: Double, areaId: String?
    ): Result<BinData> = runCatching {
        val resp = RetrofitClient.api.createBin(CreateBinRequest(nodeId, location, lat, lng, areaId))
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data.toBinData()
    }

    suspend fun updateBin(
        id: String, nodeId: String?, location: String?, lat: Double?, lng: Double?, areaId: String?
    ): Result<BinData> = runCatching {
        val resp = RetrofitClient.api.updateBin(id, UpdateBinRequest(nodeId, location, lat, lng, areaId))
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data.toBinData()
    }

    suspend fun setThreshold(
        id: String, weight: Double?, volume: Int?, gas: Double?, battery: Int?
    ): Result<Unit> = runCatching {
        val resp = RetrofitClient.api.setThreshold(id, ThresholdRequest(weight, volume, gas, battery))
        if (!resp.success) error(resp.message)
    }

    suspend fun deleteBin(id: String): Result<Unit> = runCatching {
        val resp = RetrofitClient.api.deleteBin(id)
        if (!resp.success) error(resp.message)
    }
}

// ── Mapping helpers ────────────────────────────────────────────────────────────

fun ApiBin.toBinData(): BinData {
    val vol = latest?.volume ?: 0
    return BinData(
        id         = id,
        nodeId     = nodeId,
        location   = location,
        area       = area?.name ?: areaId ?: "",
        areaId     = areaId,
        capacity   = vol,
        status     = vol.toStatus(),
        lastUpdate = latest?.timestamp?.toRelativeTime() ?: "Tidak diketahui",
        battery    = latest?.battery ?: 0,
        gas        = latest?.gas ?: 0.0,
        lat        = lat,
        lng        = lng,
        alertText  = if (vol >= 90) "Segera dikosongkan" else if (vol >= 70) "Perlu dikosongkan" else "",
        online     = !status.equals("offline", ignoreCase = true)
    )
}

fun ApiBinDetail.toBinData(): BinData {
    val vol = latest?.volume ?: 0
    return BinData(
        id         = id,
        nodeId     = nodeId,
        location   = location,
        area       = area?.name ?: areaId ?: "",
        areaId     = areaId,
        capacity   = vol,
        status     = vol.toStatus(),
        lastUpdate = latest?.timestamp?.toRelativeTime() ?: "Tidak diketahui",
        battery    = latest?.battery ?: 0,
        gas        = latest?.gas ?: 0.0,
        lat        = lat,
        lng        = lng,
        alertText  = if (vol >= 90) "Segera dikosongkan" else if (vol >= 70) "Perlu dikosongkan" else "",
        online     = !status.equals("offline", ignoreCase = true)
    )
}

private fun Int.toStatus() = when {
    this >= 90 -> BinStatus.CRITICAL
    this >= 70 -> BinStatus.NEED_PICKUP
    else       -> BinStatus.NORMAL
}

private fun String.toRelativeTime(): String = runCatching {
    val instant = Instant.parse(this)
    val now     = Instant.now()
    val diff    = now.epochSecond - instant.epochSecond
    when {
        diff < 60       -> "Baru saja"
        diff < 3600     -> "${diff / 60} menit yang lalu"
        diff < 86400    -> "${diff / 3600} jam yang lalu"
        else            -> "${diff / 86400} hari yang lalu"
    }
}.getOrDefault(this)
