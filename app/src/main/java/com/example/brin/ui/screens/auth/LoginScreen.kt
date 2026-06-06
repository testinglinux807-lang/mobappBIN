package com.example.brin.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.R
import com.example.brin.data.UserRole
import com.example.brin.data.repository.AuthRepository
import com.example.brin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: (String, UserRole) -> Unit) {
    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var showPass  by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()

    fun doLogin() {
        if (email.isBlank() || password.isBlank()) { errorMsg = "Email dan password tidak boleh kosong"; return }
        isLoading = true
        errorMsg  = ""
        scope.launch {
            val result = AuthRepository.login(context, email.trim(), password)
            result.fold(
                onSuccess = { user ->
                    val role = runCatching { UserRole.valueOf(user.role) }.getOrDefault(UserRole.ADMIN)
                    onLoginSuccess(user.name, role)
                },
                onFailure = {
                    errorMsg  = it.message ?: "Login gagal"
                    isLoading = false
                }
            )
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GreenPrimary,
        focusedLabelColor = GreenPrimary,
        unfocusedBorderColor = DividerColor,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = SearchBg
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(GreenDark, GreenPrimary)))
            .imePadding()
    ) {
        // Brand area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painterResource(R.drawable.logo_smartbin),
                contentDescription = "Smart BIN logo",
                modifier = Modifier.size(200.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Smart BIN", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(6.dp))
            Text("Sistem Pengelolaan Sampah Berbasis AI", fontSize = 15.sp, color = Color.White.copy(alpha = 0.75f))
        }

        // White sheet with form
        Surface(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = CardBg,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp)) {
                Text("Masuk", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Masukkan email dan password akun Anda", fontSize = 13.sp, color = TextSecondary)

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = email, onValueChange = { email = it; errorMsg = "" },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = if (email.isNotEmpty()) GreenPrimary else TextHint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = password, onValueChange = { password = it; errorMsg = "" },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (password.isNotEmpty()) GreenPrimary else TextHint) },
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = TextHint)
                        }
                    },
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); doLogin() }),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(errorMsg, fontSize = 12.sp, color = StatusCritical)
                    }
                }

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = { focusManager.clearFocus(); doLogin() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Masuk", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "Smart BIN v1.0 · BRIN",
                    fontSize = 11.sp,
                    color = TextHint,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
