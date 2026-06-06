package com.example.brin.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.brin.R

/** Notifikasi alert bin ke tray HP. Dipicu dari event WebSocket selama app hidup. */
object AlertNotifier {

    private const val CHANNEL_ID = "brin_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Peringatan Bin",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifikasi alert bin: penuh, baterai lemah, gas berbahaya" }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, notifId: Int, title: String, message: String) {
        // Android 13+ butuh izin POST_NOTIFICATIONS; kalau belum diberi, diam saja.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    fun titleFor(type: String): String = when (type) {
        "FULL_WEIGHT" -> "Bin Penuh (Berat)"
        "FULL_VOLUME" -> "Bin Penuh (Volume)"
        "BATTERY_LOW" -> "Baterai Lemah"
        "GAS_HIGH"    -> "Gas Berbahaya"
        else           -> "Peringatan Bin"
    }
}
