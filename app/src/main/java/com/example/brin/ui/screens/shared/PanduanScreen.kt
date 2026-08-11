package com.example.brin.ui.screens.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.ui.theme.*

/**
 * Panduan / buku panduan aplikasi SmartBIN — dipakai bersama Admin & Petugas.
 * Dibuka dari menu "Bantuan" (Profil petugas) & "Bantuan & Dukungan" (Pengaturan admin).
 */
@Composable
fun PanduanScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding().padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Text("Panduan Aplikasi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("SmartBIN", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Pengelolaan sampah pintar berbasis sensor", fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f))
                }
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Intro (selalu terlihat)
            Surface(shape = RoundedCornerShape(14.dp), color = GreenSurface, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Setiap tempat sampah (bin) punya sensor yang mengukur volume, berat, gas, & baterai lalu mengirim datanya secara real-time. Aplikasi ini memantau kondisi bin dan mengatur pengangkutan sampah. Ada 2 peran: Admin (mengelola sistem) dan Petugas (mengangkut sampah di lapangan).",
                        fontSize = 13.sp, color = TextPrimary, lineHeight = 19.sp
                    )
                }
            }

            GuideSection(Icons.Default.AdminPanelSettings, "Untuk Admin", startExpanded = true) {
                Bullet("Beranda — pantau semua bin lintas area, lihat peringatan kritis & alert terbaru secara real-time.")
                Bullet("Peta — lihat sebaran lokasi bin di peta.")
                Bullet("Kelola Bin — tambah, edit, atau hapus bin, serta buat QR Code untuk ditempel di bin.")
                Bullet("Kelola Petugas — tambah akun petugas dan tentukan area tanggung jawabnya.")
                Bullet("Kelola Zona/Area — buat area beserta petugas penanggung jawabnya.")
                Bullet("Notifikasi — semua alert bin; admin bisa menandai alert selesai (resolve).")
                Bullet("Pengaturan — ganti password & atur notifikasi.")
            }

            GuideSection(Icons.Default.LocalShipping, "Untuk Petugas") {
                Bullet("Beranda — pantau bin di area tanggung jawab Anda: yang perlu perhatian & status semua bin.")
                Bullet("Pickup — daftar bin yang perlu diangkut, dibagi 3 tab: Perlu Pickup, Dalam Proses, Selesai.")
                Bullet("Scan QR — pindai QR di bin untuk langsung mencatat pickup di lokasi.")
                Bullet("Notifikasi — alert khusus area Anda.")
                Bullet("Profil — info akun & statistik pickup Anda.")
                InfoNote("Anda hanya melihat bin di area yang ditugaskan admin. Jika bin milik area lain ikut muncul, hubungi admin untuk memeriksa penetapan area akun Anda.")
            }

            GuideSection(Icons.Default.Route, "Alur Pickup Sampah (Petugas)") {
                Step(1, "Bin penuh terdeteksi sensor → sistem membuat alert, bin muncul di tab \"Perlu Pickup\".")
                Step(2, "Datang ke lokasi bin, tekan \"Pickup Sekarang\" (atau Scan QR di bin). Lokasi GPS Anda dicatat & aplikasi peta terbuka untuk navigasi.")
                Step(3, "Pickup berpindah ke tab \"Dalam Proses\" — menunggu sensor mengonfirmasi bin sudah kosong.")
                Step(4, "Setelah sensor membaca bin kosong (atau Anda tekan \"Tandai Selesai (Manual)\" bila sensor bermasalah), pickup pindah ke tab \"Selesai\".")
            }

            GuideSection(Icons.Default.Sensors, "Arti Status Bin") {
                StatusLegend(StatusNormal, "Normal", "Kapasitas di bawah 70%. Belum perlu diangkut.")
                StatusLegend(StatusWarning, "Perlu Pickup", "Kapasitas 70–89%. Sebaiknya segera dijadwalkan.")
                StatusLegend(StatusCritical, "Kritis", "Kapasitas 90% ke atas. Harus segera diangkut.")
            }

            GuideSection(Icons.AutoMirrored.Filled.HelpOutline, "Pertanyaan Umum") {
                Faq("Data bin tidak muncul / kosong?", "Pastikan HP terhubung ke jaringan yang sama dengan server, dan akun Anda sudah diberi area oleh admin.")
                Faq("Kenapa saya lihat bin area lain?", "Berarti akun Anda belum di-set area. Minta admin menetapkan area pada data akun Anda.")
                Faq("Pickup tidak bisa / gagal GPS?", "Aktifkan GPS dan izinkan aplikasi mengakses lokasi, lalu coba lagi.")
                Faq("Sensor tidak konfirmasi setelah pickup?", "Gunakan tombol \"Tandai Selesai (Manual)\" pada pickup yang berstatus Dalam Proses.")
            }

            Text(
                "SmartBIN · Aplikasi Pengelolaan Sampah",
                fontSize = 11.sp, color = TextHint, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GuideSection(icon: ImageVector, title: String, startExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(startExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Surface(shape = RoundedCornerShape(14.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(GreenSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextHint, modifier = Modifier.size(22.dp).rotate(rotation))
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape).background(GreenMedium))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(GreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun StatusLegend(color: Color, label: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(desc, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
    }
}

@Composable
private fun InfoNote(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppBackground).padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = GreenMedium, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
    }
}

@Composable
private fun Faq(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(question, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(answer, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
    }
}
