package com.example.brin.util

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Menerjemahkan error teknis jadi kalimat yang dimengerti pengguna awam.
 *
 * Tanpa ini, error dari Retrofit muncul apa adanya di layar — misal "HTTP 429 Too Many Requests"
 * atau "Unable to resolve host ..." — yang tidak berarti apa-apa buat petugas di lapangan.
 *
 * @param fallback dipakai kalau error-nya tidak dikenali.
 * @param on401 pesan khusus untuk 401, karena artinya beda per konteks: di layar login berarti
 *              kredensial salah, di layar lain berarti sesi habis.
 */
fun Throwable.toUserMessage(
    fallback: String = "Terjadi kesalahan. Coba lagi sebentar lagi.",
    on401: String = "Sesi kamu sudah berakhir. Silakan masuk lagi."
): String = when (this) {
    is HttpException -> {
        // Backend selalu kirim { success, message, data }. `message` itu kalimat
        // sudah berbahasa manusia (mis. "action tidak dikenal: shutdown" atau
        // "Perangkat tidak merespons dalam 12s") — dipakai dulu sebelum nyimpulin
        // dari kode status. Tanpa ini, 502/504 tampil "Server sedang bermasalah"
        // yang menyembunyikan penyebab aslinya.
        val serverMsg = response()?.errorBody()?.string()?.let { raw ->
            runCatching {
                val obj = JsonParser.parseString(raw).asJsonObject
                obj.get("message")?.takeIf { !it.isJsonNull }?.asString
            }.getOrNull()
        }
        if (!serverMsg.isNullOrBlank() && serverMsg.isHumanReadable()) {
            serverMsg
        } else when (code()) {
            400, 422 -> "Data yang dikirim belum lengkap atau tidak sesuai. Periksa lagi isiannya."
            401      -> on401
            403      -> "Kamu tidak punya akses untuk melakukan ini."
            404      -> "Datanya tidak ditemukan. Mungkin sudah dihapus."
            409      -> "Data ini sudah ada atau sedang dipakai, jadi tidak bisa diproses."
            429      -> "Terlalu sering mencoba. Tunggu sekitar satu menit, lalu coba lagi."
            in 500..599 -> "Server sedang bermasalah. Coba lagi beberapa saat lagi."
            else     -> fallback
        }
    }
    is UnknownHostException   -> "Tidak ada koneksi internet. Periksa jaringan di HP kamu."
    is SocketTimeoutException -> "Server lama sekali merespons. Periksa koneksi, lalu coba lagi."
    is SSLException           -> "Koneksi ke server terputus. Coba lagi."
    is IOException            -> "Gagal terhubung ke server. Periksa koneksi internet kamu."
    // Sisanya biasanya pesan dari backend lewat error(resp.message) — itu sudah berbahasa manusia,
    // tapi tetap disaring supaya sisa teks teknis tidak bocor ke layar.
    else -> message?.takeIf { it.isHumanReadable() } ?: fallback
}

/** Buang pesan yang jelas-jelas teknis: kode HTTP, nama class exception, stack-ish, atau kosong. */
private fun String.isHumanReadable(): Boolean {
    val t = trim()
    if (t.isEmpty() || t.length > 200) return false
    if (t.startsWith("HTTP ", ignoreCase = true)) return false
    if (Regex("""^\d{3}\b""").containsMatchIn(t)) return false
    if (t.contains("Exception") || t.contains("java.") || t.contains("kotlin.")) return false
    if (t.contains("at com.example.brin")) return false
    return true
}
