package com.example.brin.ui.screens.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.BinStatus
import com.example.brin.data.TaskStatus
import com.example.brin.ui.theme.*

fun statusColor(status: BinStatus): Color = when (status) {
    BinStatus.CRITICAL    -> StatusCritical
    BinStatus.NEED_PICKUP -> StatusWarning
    BinStatus.NORMAL      -> StatusNormal
}

fun statusLabel(status: BinStatus): String = when (status) {
    BinStatus.CRITICAL    -> "KRITIS"
    BinStatus.NEED_PICKUP -> "PERLU"
    BinStatus.NORMAL      -> "NORMAL"
}

fun taskStatusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.PENDING     -> StatusWarning
    TaskStatus.IN_PROGRESS -> Color(0xFF1976D2)
    TaskStatus.DONE        -> StatusNormal
}

fun taskStatusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.PENDING     -> "PENDING"
    TaskStatus.IN_PROGRESS -> "PROSES"
    TaskStatus.DONE        -> "SELESAI"
}

@Composable
fun StatusBadge(status: TaskStatus) {
    val color = taskStatusColor(status)
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text       = taskStatusLabel(status),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = color,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun BinStatusBadge(status: BinStatus) {
    val color = statusColor(status)
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text       = statusLabel(status),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = color,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
