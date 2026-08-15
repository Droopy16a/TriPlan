package com.example.triplane

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.triplane.core.auth.AuthRepository
import com.triplane.core.auth.AuthState
import com.triplane.core.ai.ProfileRepository
import com.triplane.core.designsystem.theme.TripLaneTheme
import com.triplane.feature.home.BottomNavPill
import com.triplane.feature.home.HomeScreen
import com.triplane.feature.home.HomeViewModel
import com.triplane.feature.home.LoginScreen
import com.triplane.feature.home.PlannerScreen
import com.triplane.feature.home.ProfileScreen
import com.triplane.feature.trip.TripExpandOverlay
import com.triplane.feature.trip.TripWorkspaceScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripLaneTheme {
                // Observe auth state from Supabase session
                val authState by AuthRepository.authState.collectAsState(initial = AuthState.Loading)
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                // When authenticated, hydrate ProfileRepository with Google account data
                LaunchedEffect(authState) {
                    if (authState is AuthState.Authenticated) {
                        val state = authState as AuthState.Authenticated
                        ProfileRepository.loadFromAuth(
                            name = state.name,
                            email = state.email,
                            avatarUrl = state.avatarUrl,
                            birthDate = state.birthDate,
                            phoneCountryCode = state.phoneCountryCode,
                            phoneNumber = state.phoneNumber
                        )
                    }
                }

                when (authState) {
                    is AuthState.Loading -> {
                        // Splash / loading — show nothing (system splash screen handles this)
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    is AuthState.Unauthenticated -> {
                        LoginScreen()
                    }

                    is AuthState.Authenticated -> {
                        MainAppContent(
                            onSignOut = {
                                scope.launch { AuthRepository.signOut(context) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainAppContent(onSignOut: () -> Unit) {
    val homeViewModel: HomeViewModel = viewModel()
    var currentScreen by remember { mutableStateOf("home") }
    var previousScreen by remember { mutableStateOf("home") }
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    var selectedTripId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentScreen) {
        kotlinx.coroutines.delay(100)
        previousScreen = currentScreen
    }

    // Intercept back button during expanding animation
    BackHandler(enabled = currentScreen == "expanding") {
        currentScreen = "home"
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Planner Screen: Kept in cache for smoothness (not disposed)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (currentScreen == "planner") 1.2f else 0f)
                .graphicsLayer {
                    alpha = if (currentScreen == "planner") 1f else 0f
                }
        ) {
            PlannerScreen(
                isActive = currentScreen == "planner",
                onBack = { currentScreen = "home" },
                viewModel = homeViewModel
            )
        }

        // Bottom layer: TripWorkspaceScreen
        AnimatedVisibility(
            visible = currentScreen == "expanding" || currentScreen == "trip",
            modifier = Modifier.zIndex(if (currentScreen == "trip") 1f else 0f),
            enter = fadeIn(tween(400)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(500)
            ) + fadeOut(tween(500))
        ) {
            TripWorkspaceScreen(
                tripId = selectedTripId,
                transitionComplete = currentScreen == "trip",
                onBackClick = { currentScreen = "home" }
            )
        }

        // Middle layer: HomeScreen (Cached to keep minimaps warm)
        val isHomeActive = currentScreen == "home" || currentScreen == "expanding"
        val homeAlpha by animateFloatAsState(
            targetValue = if (isHomeActive) 1f else 0f,
            animationSpec = if (currentScreen == "planner" || previousScreen == "planner") snap() else tween(400),
            label = "homeAlpha"
        )
        val homeTranslationX by animateFloatAsState(
            targetValue = if (isHomeActive) 0f else if (currentScreen == "trip") -screenWidthPx else 0f,
            animationSpec = if (currentScreen == "trip" || previousScreen == "trip") tween(500) else snap(),
            label = "homeTranslationX"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (isHomeActive) 1.1f else 0f)
                .graphicsLayer {
                    alpha = homeAlpha
                    translationX = homeTranslationX
                }
        ) {
            HomeScreen(
                onPlanTripClick = { },
                onTripClick = { bounds, tripId ->
                    cardBounds = bounds
                    selectedTripId = tripId
                    currentScreen = "expanding"
                    homeViewModel.setSearchFormExpanded(false)
                },
                viewModel = homeViewModel
            )
        }

        // Profile Screen
        val isProfileActive = currentScreen == "profile"
        val profileAlpha by animateFloatAsState(
            targetValue = if (isProfileActive) 1f else 0f,
            animationSpec = tween(400),
            label = "profileAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (isProfileActive) 1.15f else 0f)
                .graphicsLayer {
                    alpha = profileAlpha
                }
        ) {
            ProfileScreen(onSignOut = onSignOut)
        }

        // Persistent Bottom Nav
        AnimatedVisibility(
            visible = currentScreen == "home" || currentScreen == "planner" || currentScreen == "profile",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            BottomNavPill(
                currentScreen = currentScreen,
                onTabClick = { screen ->
                    if (currentScreen != screen) {
                        currentScreen = screen
                        homeViewModel.setSearchFormExpanded(false)
                    }
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }

        // Top layer: overlay expands from card bounds, on top of HomeScreen
        if (currentScreen == "expanding") {
            Box(modifier = Modifier.zIndex(2f)) {
                TripExpandOverlay(
                    tripId = selectedTripId,
                    startBounds = cardBounds,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    onComplete = { currentScreen = "trip" }
                )
            }
        }
    }
}
