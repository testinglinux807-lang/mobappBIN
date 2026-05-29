package com.example.brin.data.repository

import com.example.brin.data.api.ApiArea
import com.example.brin.data.api.AreaRequest
import com.example.brin.data.api.RetrofitClient

object AreaRepository {

    suspend fun getAreas(): Result<List<ApiArea>> = runCatching {
        val resp = RetrofitClient.api.getAreas()
        if (!resp.success) error("Gagal memuat areas")
        resp.data.orEmpty()
    }

    suspend fun createArea(name: String): Result<ApiArea> = runCatching {
        val resp = RetrofitClient.api.createArea(AreaRequest(name))
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data
    }

    suspend fun updateArea(id: String, name: String): Result<ApiArea> = runCatching {
        val resp = RetrofitClient.api.updateArea(id, AreaRequest(name))
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data
    }

    suspend fun deleteArea(id: String): Result<Unit> = runCatching {
        val resp = RetrofitClient.api.deleteArea(id)
        if (!resp.success) error(resp.message)
    }
}
