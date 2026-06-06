package com.example.brin.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.R
import com.example.brin.data.UserRole
import com.example.brin.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.brin.ui.theme.GreenDark
import com.example.brin.ui.theme.GreenPrimary

@Composable
fun SplashScreen(
    onAutoLogin: (name: String, role: UserRole) -> Unit,
    onNeedLogin: () -> Unit
) {
    val context     = LocalContext.current
    val contentAlpha = remember { Animatable(0f) }
    val logoScale    = remember { Animatable(0.88f) }
    val progress     = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Smooth, calm entrance — no bounce
        launch { contentAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { progress.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
        logoScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        delay(700)

        // Try restoring saved session
        val user = AuthRepository.restoreSession(context)
        if (user != null) {
            val role = runCatching { UserRole.valueOf(user.role) }.getOrDefault(UserRole.ADMIN)
            onAutoLogin(user.name, role)
        } else {
            onNeedLogin()
        }
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
            Image(
                painterResource(R.drawable.logo_smartbin),
                contentDescription = "Smart BIN logo",
                modifier = Modifier.size(116.dp).scale(logoScale.value)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Smart BIN",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Sistem Pengelolaan Sampah Berbasis AI",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
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
            Spacer(Modifier.height(18.dp))
            Text(
                "MADE BY BRIN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.55f),
                letterSpacing = 4.sp
            )
        }
    }
}
