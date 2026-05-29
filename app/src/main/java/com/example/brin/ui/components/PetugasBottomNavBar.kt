package com.example.brin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.brin.ui.navigation.PetugasScreen
import com.example.brin.ui.theme.*

private data class PetugasNavItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun PetugasBottomNavBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route

    val items = listOf(
        PetugasNavItem(PetugasScreen.Home.route,    Icons.Default.Home,          "Beranda"),
        PetugasNavItem(PetugasScreen.Pickup.route,  Icons.Default.LocalShipping, "Pickup"),
        PetugasNavItem(PetugasScreen.Scan.route,    Icons.Default.QrCodeScanner, "Scan QR"),
        PetugasNavItem(PetugasScreen.Jadwal.route,  Icons.Default.CalendarMonth, "Jadwal"),
        PetugasNavItem(PetugasScreen.Profile.route, Icons.Default.Person,        "Profil"),
    )

    val activeIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(32.dp), color = CardBg, shadowElevation = 20.dp, modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                val itemWidth    = maxWidth / items.size
                val indicatorOff by animateDpAsState(
                    targetValue   = itemWidth * activeIndex,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label         = "petugasNavIndicator"
                )
                Box(modifier = Modifier.offset(x = indicatorOff).width(itemWidth).height(44.dp).padding(horizontal = 4.dp).clip(RoundedCornerShape(22.dp)).background(GreenPrimary))
                Row(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        val isActive = activeIndex == index
                        Box(
                            modifier = Modifier.width(itemWidth).height(44.dp).clip(RoundedCornerShape(22.dp)).clickable(
                                interactionSource = remember { MutableInteractionSource() }, indication = null
                            ) { navController.navigate(item.route) { popUpTo(PetugasScreen.Home.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = item.label, tint = if (isActive) Color.White else TextHint, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}
