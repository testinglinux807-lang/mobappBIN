package com.example.brin.data.local

import com.example.brin.data.api.ApiUser

object AppState {
    var token: String? = null
    var currentUser: ApiUser? = null

    fun clear() {
        token = null
        currentUser = null
    }

    val userName: String get() = currentUser?.name ?: "Pengguna"
    val userEmail: String get() = currentUser?.email ?: ""
    val userRole: String get() = currentUser?.role ?: "ADMIN"
    val areaId: String? get() = currentUser?.areaId
}
