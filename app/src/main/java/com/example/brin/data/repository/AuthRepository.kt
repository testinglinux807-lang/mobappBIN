package com.example.brin.data.repository

import android.content.Context
import com.example.brin.data.api.ApiUser
import com.example.brin.data.api.ChangePasswordRequest
import com.example.brin.data.api.LoginRequest
import com.example.brin.data.api.RetrofitClient
import com.example.brin.data.local.AppState
import com.example.brin.data.local.TokenStorage

object AuthRepository {

    suspend fun login(context: Context, email: String, password: String): Result<ApiUser> = runCatching {
        val resp = RetrofitClient.api.login(LoginRequest(email, password))
        if (!resp.success || resp.data == null) error(resp.message)
        val token = resp.data.token
        val user  = resp.data.user
        AppState.token       = token
        AppState.currentUser = user
        TokenStorage(context).saveToken(token)
        user
    }

    suspend fun getMe(): ApiUser? = runCatching {
        val resp = RetrofitClient.api.getMe()
        if (resp.success && resp.data != null) {
            AppState.currentUser = resp.data
            resp.data
        } else null
    }.getOrNull()

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> = runCatching {
        val resp = RetrofitClient.api.changePassword(ChangePasswordRequest(oldPassword, newPassword))
        if (!resp.success) error(resp.message)
    }

    suspend fun logout(context: Context) {
        TokenStorage(context).clearToken()
        AppState.clear()
    }

    suspend fun restoreSession(context: Context): ApiUser? {
        val saved = TokenStorage(context).getToken() ?: return null
        AppState.token = saved
        val user = getMe()
        if (user == null) {
            TokenStorage(context).clearToken()
            AppState.clear()
        }
        return user
    }
}
