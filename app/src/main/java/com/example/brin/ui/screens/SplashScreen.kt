package com.example.brin.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val logoScale  = remember { Animatable(0.4f) }
    val logoAlpha  = remember { Animatable(0f) }
    val textAlpha  = remember { Animatable(0f) }
    val dotAlpha1  = remember { Animatable(0f) }
    val dotAlpha2  = remember { Animatable(0f) }
    val dotAlpha3  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Logo muncul + scale up
        logoAlpha.animateTo(1f, tween(500))
        logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))

        // Teks muncul
        textAlpha.animateTo(1f, tween(400))

        delay(400)

        // Loading dots berurutan
        dotAlpha1.animateTo(1f, tween(200))
        dotAlpha2.animateTo(1f, tween(200))
        dotAlpha3.animateTo(1f, tween(200))

        delay(800)

        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GreenDark, GreenPrimary, GreenMedium)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative circles background
        Box(
            modifier = Modifier
                .size(320.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
                .align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .align(Alignment.Center)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo icon
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(58.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // App name
            Column(
                modifier            = Modifier.alpha(textAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Smart BIN",
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    "Sistem Pengelolaan Sampah",
                    fontSize  = 14.sp,
                    color     = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        "BRIN",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        letterSpacing = 3.sp,
                        modifier   = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(60.dp))

            // Loading dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                LoadingDot(alpha = dotAlpha1.value)
                LoadingDot(alpha = dotAlpha2.value)
                LoadingDot(alpha = dotAlpha3.value)
            }
        }

        // Bottom version text
        Text(
            "v1.0.0",
            fontSize = 11.sp,
            color    = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(textAlpha.value)
        )
    }
}

@Composable
private fun LoadingDot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.8f))
    )
}
