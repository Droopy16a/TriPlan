package com.ramble.feature.home

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramble.core.auth.AuthRepository
import com.ramble.core.designsystem.component.RambleButton
import com.ramble.core.designsystem.component.RambleCard
import com.ramble.core.designsystem.theme.DeepGraphite
import com.ramble.core.designsystem.theme.OffWhite
import kotlinx.coroutines.launch

enum class LoginStep {
    WELCOME, EMAIL, DETAILS
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(LoginStep.WELCOME) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .alpha(contentAlpha)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Header with Back Button (if not on Welcome)
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep != LoginStep.WELCOME) {
                    IconButton(onClick = {
                        errorMessage = null
                        currentStep = when (currentStep) {
                            LoginStep.DETAILS -> LoginStep.EMAIL
                            LoginStep.EMAIL -> LoginStep.WELCOME
                            LoginStep.WELCOME -> LoginStep.WELCOME
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepGraphite)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DeepGraphite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = com.ramble.core.designsystem.R.drawable.ic_logo),
                    contentDescription = "Ramble Logo",
                    modifier = Modifier.size(54.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Ramble",
                fontSize = 32.sp,
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

            // Main Content Card
            RambleCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                containerColor = Color.White
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }.using(SizeTransform(clip = false))
                    },
                    label = "stepTransition"
                ) { step ->
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (step) {
                            LoginStep.WELCOME -> WelcomeStep(
                                onContinueWithGoogle = {
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            AuthRepository.signInWithGoogle(context)
                                        } catch (e: Exception) {
                                            errorMessage = if (e.message?.contains("cancel", true) == true) null else "Sign-in failed."
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                onContinueWithEmail = { currentStep = LoginStep.EMAIL },
                                isLoading = isLoading
                            )
                            LoginStep.EMAIL -> EmailStep(
                                email = email,
                                onEmailChange = { email = it },
                                onNext = {
                                    if (email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                        errorMessage = null
                                        currentStep = LoginStep.DETAILS
                                    } else {
                                        errorMessage = "Please enter a valid email."
                                    }
                                }
                            )
                            LoginStep.DETAILS -> DetailsStep(
                                isSignUpMode = isSignUpMode,
                                onToggleMode = { isSignUpMode = !isSignUpMode },
                                firstName = firstName,
                                onFirstNameChange = { firstName = it },
                                lastName = lastName,
                                onLastNameChange = { lastName = it },
                                password = password,
                                onPasswordChange = { password = it },
                                isLoading = isLoading,
                                onAction = {
                                    scope.launch {
                                        if (password.isBlank()) {
                                            errorMessage = "Please enter a password."
                                            return@launch
                                        }
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            if (isSignUpMode) {
                                                if (firstName.isBlank() || lastName.isBlank()) {
                                                    errorMessage = "Please fill in your name."
                                                    isLoading = false
                                                    return@launch
                                                }
                                                AuthRepository.signUpWithEmail(email, password, firstName, lastName)
                                                errorMessage = "Verification email sent!"
                                            } else {
                                                AuthRepository.signInWithEmail(email, password)
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Authentication failed."
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Step Indicator
            if (currentStep != LoginStep.WELCOME) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        val active = (currentStep == LoginStep.EMAIL && index == 0) || (currentStep == LoginStep.DETAILS && index == 1)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (active) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (active) DeepGraphite else Color(0xFFE0E0E0))
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
                    textAlign = TextAlign.Center,
                    modifier = Modifier.animateContentSize()
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

@Composable
fun WelcomeStep(
    onContinueWithGoogle: () -> Unit,
    onContinueWithEmail: () -> Unit,
    isLoading: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                .clickable(enabled = !isLoading) { onContinueWithGoogle() },
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
                } else {
                    // Google "G" logo in color
                    Text(text = "G", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Continue with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGraphite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RambleButton(
            onClick = onContinueWithEmail,
            text = "Continue with Email",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
            }
        )
    }
}

@Composable
fun EmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "What's your email?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DeepGraphite
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeepGraphite,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedLabelColor = DeepGraphite
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        RambleButton(
            onClick = onNext,
            text = "Next",
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank()
        )
    }
}

@Composable
fun DetailsStep(
    isSignUpMode: Boolean,
    onToggleMode: () -> Unit,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    onAction: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isSignUpMode) "Create your account" else "Welcome back!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DeepGraphite
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isSignUpMode) {
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
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

            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
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

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
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

        RambleButton(
            onClick = onAction,
            text = if (isSignUpMode) "Sign Up" else "Sign In",
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSignUpMode) "Already have an account? Sign In" else "New here? Create Account",
            modifier = Modifier.clickable { onToggleMode() },
            color = DeepGraphite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
