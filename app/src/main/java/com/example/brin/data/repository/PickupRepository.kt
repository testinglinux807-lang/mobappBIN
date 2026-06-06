package com.example.brin.data.repository

import com.example.brin.data.api.ApiPickup
import com.example.brin.data.api.CompletePickupRequest
import com.example.brin.data.api.RetrofitClient

object PickupRepository {

    suspend fun completePickup(binId: String, lat: Double? = null, lng: Double? = null): Result<ApiPickup> = runCatching {
        val resp = RetrofitClient.api.completePickup(binId, CompletePickupRequest(lat, lng))
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data
    }

    suspend fun getPickups(): Result<List<ApiPickup>> = runCatching {
        val resp = RetrofitClient.api.getPickups()
        if (!resp.success) error("Gagal memuat pickups")
        resp.data.orEmpty()
    }

    suspend fun getPickupById(id: String): Result<ApiPickup> = runCatching {
        val resp = RetrofitClient.api.getPickupById(id)
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data
    }

    /** Konfirmasi manual (fallback saat sensor error) — tandai pickup SELESAI. */
    suspend fun confirmPickup(id: String): Result<ApiPickup> = runCatching {
        val resp = RetrofitClient.api.confirmPickup(id)
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data
    }
}
