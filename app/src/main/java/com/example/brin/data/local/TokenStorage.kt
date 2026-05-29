package com.example.brin.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "brin_session")

class TokenStorage(private val context: Context) {

    private val KEY_TOKEN = stringPreferencesKey("auth_token")

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun getToken(): String? =
        context.dataStore.data.map { it[KEY_TOKEN] }.firstOrNull()

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(KEY_TOKEN) }
    }
}
