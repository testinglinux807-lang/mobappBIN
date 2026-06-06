package com.example.brin.ui.screens.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.ui.theme.*

@Composable
fun DetailRuteScreen(
    routeId: String,
    onBack: () -> Unit,
    onBinClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Surface(color = CardBg) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Text("Detail Rute", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Rute tidak ditemukan", color = TextHint, fontSize = 14.sp)
        }
    }
}
