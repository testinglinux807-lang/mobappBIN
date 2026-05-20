package com.example.brin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.brin.data.MockData
import com.example.brin.ui.navigation.Screen
import com.example.brin.ui.theme.*

private data class NavItem(val route: String, val icon: ImageVector, val label: String, val badge: Int = 0)

@Composable
fun AppBottomNavBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    val unreadNotif    = MockData.notifications.count { !it.isRead }

    val items = listOf(
        NavItem(Screen.Home.route,          Icons.Default.Dashboard,     "Dashboard"),
        NavItem(Screen.Map.route,           Icons.Default.Explore,       "Explore"),
        NavItem(Screen.Notifications.route, Icons.Default.Notifications, "Notifikasi", unreadNotif),
        NavItem(Screen.Schedule.route,       Icons.Default.CalendarMonth, "Jadwal"),
        NavItem(Screen.Settings.route,      Icons.Default.Person,        "Profil"),
    )

    val activeIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Box(
        modifier        = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape           = RoundedCornerShape(32.dp),
            color           = CardBg,
            shadowElevation = 20.dp,
            modifier        = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                val itemWidth    = maxWidth / items.size
                val indicatorOff by animateDpAsState(
                    targetValue   = itemWidth * activeIndex,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    label = "navIndicator"
                )

                // Sliding green pill
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOff)
                        .width(itemWidth)
                        .height(44.dp)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(GreenPrimary)
                )

                // Nav items
                Row(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, item ->
                        val isActive = activeIndex == index
                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null
                                ) { navigate(navController, item.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier              = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Box {
                                    Icon(
                                        imageVector        = item.icon,
                                        contentDescription = item.label,
                                        tint               = if (isActive) Color.White else TextHint,
                                        modifier           = Modifier.size(22.dp)
                                    )
                                    if (item.badge > 0 && !isActive) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(StatusCritical)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun navigate(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState    = true
    }
}
