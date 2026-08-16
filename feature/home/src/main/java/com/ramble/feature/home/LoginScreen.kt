package com.ramble.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramble.core.auth.AuthRepository
import com.ramble.core.designsystem.theme.DeepGraphite
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }

    // Animate content in on first composition
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700),
        label = "loginAlpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = tween(700),
        label = "loginScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .alpha(contentAlpha)
                .scale(contentScale)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // App Logo / Icon placeholder
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(DeepGraphite),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✈",
                    fontSize = 44.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Ramble",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGraphite,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI-powered travel planning,\npersonalized for you.",
                fontSize = 15.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (isSignUpMode) {
                // First Name Field
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepGraphite,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedLabelColor = DeepGraphite
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Last Name Field
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepGraphite,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedLabelColor = DeepGraphite
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepGraphite,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedLabelColor = DeepGraphite
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepGraphite,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedLabelColor = DeepGraphite
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action Button (Sign In / Sign Up)
            Button(
                onClick = {
                    scope.launch {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill in all fields."
                            return@launch
                        }
                        isLoading = true
                        errorMessage = null
                        try {
                            if (isSignUpMode) {
                                if (firstName.isBlank() || lastName.isBlank()) {
                                    errorMessage = "Please fill in all fields."
                                    isLoading = false
                                    return@launch
                                }
                                AuthRepository.signUpWithEmail(email, password, firstName, lastName)
                                errorMessage = "Account created! Please check your email if verification is required."
                            } else {
                                AuthRepository.signInWithEmail(email, password)
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Authentication failed."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepGraphite),
                enabled = !isLoading
            ) {
                if (isLoading && !isSignUpMode) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isSignUpMode) "Create Account" else "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Toggle
            Text(
                text = if (isSignUpMode) "Already have an account? Sign In" else "New to Ramble? Create Account",
                modifier = Modifier.clickable { isSignUpMode = !isSignUpMode },
                color = DeepGraphite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // OR Separator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                Text(
                    text = "or",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFF888888),
                    fontSize = 14.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Google Sign-In Button
            val buttonScale by animateFloatAsState(
                targetValue = if (isLoading) 0.97f else 1f,
                animationSpec = tween(150),
                label = "btnScale"
            )

            Box(
                modifier = Modifier
                    .scale(buttonScale)
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                    .clickable(enabled = !isLoading) {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                AuthRepository.signInWithGoogle(context)
                                // AuthState flow in MainActivity will automatically navigate
                            } catch (e: Exception) {
                                errorMessage = when {
                                    e.message?.contains("cancel", ignoreCase = true) == true -> null
                                    else -> "Sign-in failed. Please try again."
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = DeepGraphite
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Signing in…",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = DeepGraphite
                        )
                    } else {
                        // Google "G" logo in color
                        Text(text = "G", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Continue with Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = DeepGraphite
                        )
                    }
                }
            }

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFE53935),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "By continuing, you agree to our Terms of Service\nand Privacy Policy.",
                fontSize = 12.sp,
                color = Color(0xFFBBBBBB),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
