package com.example.brin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brin.ui.theme.*

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    fun doLogin() {
        if (email.isBlank() || password.isBlank()) {
            errorMsg = "Email dan password tidak boleh kosong"
            return
        }
        if (!email.contains("@")) {
            errorMsg = "Format email tidak valid"
            return
        }
        if (password.length < 6) {
            errorMsg = "Password minimal 6 karakter"
            return
        }
        errorMsg  = ""
        isLoading = true
        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        onLoginSuccess(name)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GreenDark, GreenPrimary, GreenMedium)
                )
            )
    ) {
        // Top decorative circles
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = 40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Smart BIN",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Text(
                "Sistem Pengelolaan Sampah BRIN",
                fontSize  = 13.sp,
                color     = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // Card login
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = CardBg
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Masuk sebagai Admin",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Text(
                        "Masukkan kredensial akun admin Anda",
                        fontSize = 12.sp,
                        color    = TextSecondary
                    )

                    Spacer(Modifier.height(20.dp))

                    // Email field
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it; errorMsg = "" },
                        label         = { Text("Email") },
                        leadingIcon   = {
                            Icon(Icons.Default.Email, contentDescription = null,
                                tint = if (email.isNotEmpty()) GreenPrimary else TextHint)
                        },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GreenPrimary,
                            focusedLabelColor    = GreenPrimary,
                            unfocusedBorderColor = DividerColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Password field
                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it; errorMsg = "" },
                        label         = { Text("Password") },
                        leadingIcon   = {
                            Icon(Icons.Default.Lock, contentDescription = null,
                                tint = if (password.isNotEmpty()) GreenPrimary else TextHint)
                        },
                        trailingIcon  = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    imageVector = if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextHint
                                )
                            }
                        },
                        visualTransformation = if (showPass) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus(); doLogin() }
                        ),
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GreenPrimary,
                            focusedLabelColor    = GreenPrimary,
                            unfocusedBorderColor = DividerColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error message
                    if (errorMsg.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null,
                                tint = StatusCritical, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(errorMsg, fontSize = 12.sp, color = StatusCritical)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick  = { focusManager.clearFocus(); doLogin() },
                        enabled  = !isLoading,
                        colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color    = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Login, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Masuk", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Smart BIN v1.0 · BRIN",
                fontSize = 11.sp,
                color    = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
