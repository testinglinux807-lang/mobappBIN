package com.example.brin.ui.screens

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(adminName: String, onFinished: () -> Unit) {
    val checkScale  = remember { Animatable(0f) }
    val checkAlpha  = remember { Animatable(0f) }
    val ringScale   = remember { Animatable(0f) }
    val ringAlpha   = remember { Animatable(0f) }
    val textAlpha   = remember { Animatable(0f) }
    val cardOffsetY = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        // Ripple ring muncul duluan
        ringAlpha.animateTo(0.4f, tween(300))
        ringScale.animateTo(1f, tween(400, easing = FastOutSlowInEasing))

        // Centang muncul dengan bounce
        checkAlpha.animateTo(1f, tween(200))
        checkScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )

        // Ring fade out sedikit
        ringAlpha.animateTo(0.15f, tween(300))

        // Teks slide up + fade in
        cardOffsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(400))

        delay(1500)

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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Check icon with ripple ring
            Box(contentAlignment = Alignment.Center) {
                // Outer ripple ring
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(ringScale.value)
                        .alpha(ringAlpha.value)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                // Inner circle with checkmark
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(checkScale.value)
                        .alpha(checkAlpha.value)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = null,
                        tint               = GreenPrimary,
                        modifier           = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Welcome text card
            Column(
                modifier            = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = cardOffsetY.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Selamat Datang!",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    adminName,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        "Administrator · Sistem BRIN",
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(48.dp))

                // Loading indicator
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color       = Color.White.copy(alpha = 0.7f),
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        strokeCap   = StrokeCap.Round
                    )
                    Text(
                        "Memuat dashboard...",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
