package com.example.brin.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.UserRole
import com.example.brin.ui.theme.GreenDark
import com.example.brin.ui.theme.GreenPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(userName: String, userRole: UserRole, onFinished: () -> Unit) {
    val checkScale  = remember { Animatable(0.7f) }
    val contentAlpha = remember { Animatable(0f) }
    val ringAlpha   = remember { Animatable(0f) }
    val progress    = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { ringAlpha.animateTo(0.15f, tween(500)) }
        launch { contentAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        launch { progress.animateTo(1f, tween(1800, easing = FastOutSlowInEasing)) }
        checkScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        delay(1700)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(GreenDark, GreenPrimary))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(contentAlpha.value)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(124.dp).alpha(ringAlpha.value).clip(CircleShape).background(Color.White))
                Box(
                    modifier = Modifier.size(84.dp).scale(checkScale.value).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(44.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            Text("Selamat Datang!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(userName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.15f)) {
                Text(
                    if (userRole == UserRole.ADMIN) "Administrator · Sistem BRIN" else "Petugas Lapangan · Sistem BRIN",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(contentAlpha.value)
        ) {
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .width(140.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White.copy(alpha = 0.9f),
                trackColor = Color.White.copy(alpha = 0.18f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(14.dp))
            Text(
                if (userRole == UserRole.ADMIN) "Memuat dashboard..." else "Memuat aplikasi...",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
