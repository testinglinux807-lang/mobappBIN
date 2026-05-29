package com.example.brin.data.repository

import com.example.brin.data.api.ApiUser
import com.example.brin.data.api.CreateUserRequest
import com.example.brin.data.api.RetrofitClient
import com.example.brin.data.api.UpdateUserRequest

object UserRepository {

    suspend fun getUsers(): Result<List<ApiUser>> = runCatching {
        val resp = RetrofitClient.api.getUsers()
        if (!resp.success) error("Gagal memuat users")
        resp.data.orEmpty()
    }

    suspend fun getUserById(id: String): Result<ApiUser> = runCatching {
        val resp = RetrofitClient.api.getUserById(id)
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data
    }

    suspend fun createUser(
        name: String, email: String, password: String, role: String, areaId: String?
    ): Result<ApiUser> = runCatching {
        val resp = RetrofitClient.api.createUser(CreateUserRequest(name, email, password, role, areaId))
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data
    }

    suspend fun updateUser(
        id: String, name: String?, email: String?, password: String?, role: String?, areaId: String?
    ): Result<ApiUser> = runCatching {
        val resp = RetrofitClient.api.updateUser(id, UpdateUserRequest(name, email, password, role, areaId))
        if (!resp.success || resp.data == null) error(resp.message)
        resp.data
    }

    suspend fun deleteUser(id: String): Result<Unit> = runCatching {
        val resp = RetrofitClient.api.deleteUser(id)
        if (!resp.success) error(resp.message)
    }
}
