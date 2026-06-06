package com.example.brin.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notifDataStore: DataStore<Preferences> by preferencesDataStore(name = "brin_notif")

/**
 * Menyimpan id alert yang sudah "dilihat" petugas (saat tab Notifikasi dibuka) ke DataStore,
 * supaya badge bubble tetap hilang walau app ditutup & dibuka lagi.
 */
class NotifSeenStorage(private val context: Context) {

    private val KEY_SEEN = stringSetPreferencesKey("seen_alert_ids")

    val seenIds: Flow<Set<String>> = context.notifDataStore.data.map { it[KEY_SEEN] ?: emptySet() }

    suspend fun markSeen(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.notifDataStore.edit { prefs ->
            prefs[KEY_SEEN] = (prefs[KEY_SEEN] ?: emptySet()) + ids
        }
    }
}
