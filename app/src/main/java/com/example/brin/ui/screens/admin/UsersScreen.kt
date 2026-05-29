package com.example.brin.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.data.api.ApiUser
import com.example.brin.data.repository.UserRepository
import com.example.brin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun UsersScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var users     by remember { mutableStateOf<List<ApiUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }
    var deleteTarget  by remember { mutableStateOf<ApiUser?>(null) }
    var isDeleting    by remember { mutableStateOf(false) }
    var deleteError   by remember { mutableStateOf<String?>(null) }

    fun loadUsers() {
        isLoading = true; errorMsg = null
        scope.launch {
            UserRepository.getUsers()
                .onSuccess { users = it }
                .onFailure { errorMsg = it.message }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadUsers() }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().background(GreenDark).statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White) }
                Text("Manajemen Pengguna", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(start = 4.dp))
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
                errorMsg != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMsg!!, color = StatusCritical, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { loadUsers() }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text("Coba Lagi") }
                    }
                }
                else -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 80.dp)) {
                    if (users.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada pengguna.", color = TextHint, fontSize = 13.sp)
                        }
                    } else {
                        users.forEach { user ->
                            UserCard(user = user, onEdit = { onEdit(user.id) }, onDelete = { deleteTarget = user })
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onAdd,
                containerColor = GreenPrimary, contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Tambah Pengguna") }
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { user ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) { deleteTarget = null; deleteError = null } },
            containerColor   = CardBg,
            title = { Text("Hapus Pengguna?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text("${user.name} (${user.email}) akan dihapus secara permanen.", color = TextSecondary, fontSize = 14.sp)
                    deleteError?.let { Spacer(Modifier.height(8.dp)); Text(it, color = StatusCritical, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true; deleteError = null
                        scope.launch {
                            UserRepository.deleteUser(user.id)
                                .onSuccess { users = users.filter { it.id != user.id }; deleteTarget = null }
                                .onFailure { deleteError = it.message ?: "Gagal menghapus"; isDeleting = false }
                            isDeleting = false
                        }
                    },
                    enabled = !isDeleting,
                    colors  = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                ) {
                    if (isDeleting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null; deleteError = null }, enabled = !isDeleting) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun UserCard(user: ApiUser, onEdit: () -> Unit, onDelete: () -> Unit) {
    val roleColor = if (user.role == "ADMIN") Color(0xFF1976D2) else GreenPrimary
    val roleBg    = if (user.role == "ADMIN") Color(0xFF1976D2).copy(alpha = 0.12f) else GreenPrimary.copy(alpha = 0.12f)

    Surface(shape = RoundedCornerShape(12.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(GreenPrimary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(user.name.firstOrNull()?.uppercase() ?: "?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(user.email, fontSize = 12.sp, color = TextSecondary)
                if (user.area != null) {
                    Spacer(Modifier.height(2.dp))
                    Text("Area: ${user.area.name}", fontSize = 11.sp, color = TextHint)
                }
                Spacer(Modifier.height(5.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = roleBg) {
                    Text(user.role, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = roleColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit,   contentDescription = "Edit",  tint = TextHint, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = StatusCritical, modifier = Modifier.size(20.dp)) }
        }
    }
}
