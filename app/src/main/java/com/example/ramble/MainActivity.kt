package com.example.ramble

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramble.core.auth.AuthRepository
import com.ramble.core.auth.AuthState
import com.ramble.core.ai.ProfileRepository
import com.ramble.core.ai.TripRepository
import com.ramble.core.designsystem.theme.RambleTheme
import com.ramble.feature.home.BottomNavPill
import com.ramble.feature.home.ExploreScreen
import com.ramble.feature.home.HomeScreen
import com.ramble.feature.home.HomeViewModel
import com.ramble.feature.home.LoginScreen
import com.ramble.feature.home.PlannerScreen
import com.ramble.feature.home.ProfileScreen
import com.ramble.feature.trip.TripExpandOverlay
import com.ramble.feature.trip.TripWorkspaceScreen
import kotlinx.coroutines.launch
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder

class MainActivity : ComponentActivity() {
    private val deepLinkTripId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ramble.widget.WidgetUpdater.init(this)
        TripRepository.initContext(this)
        handleIntent(intent)

        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
        Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        setContent {
             RambleTheme {
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
                        // Splash / loading
                        Box(modifier = Modifier.fillMaxSize().background(com.ramble.core.designsystem.theme.OffWhite))
                    }

                    is AuthState.Unauthenticated -> {
                        LoginScreen()
                    }

                    is AuthState.Authenticated -> {
                        MainAppContent(
                            onSignOut = {
                                scope.launch { AuthRepository.signOut(context) }
                            },
                            initialDeepLinkTripId = deepLinkTripId.value,
                            onDeepLinkHandled = { deepLinkTripId.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Log.d("RAMBLE_DEEPLINK", "handleIntent: action=${intent?.action}, data=${intent?.data}")
        if (intent?.action == Intent.ACTION_VIEW) {
            val data = intent.data
            // Support both www.rambletrip.com and rambletrip.com
            if ((data?.host == "www.rambletrip.com" || data?.host == "rambletrip.com") && 
                data.path?.startsWith("/join") == true) {
                
                // Robust path segment extraction
                val segments = data.pathSegments
                val tripId = segments.lastOrNull { it != "join" && it.isNotBlank() }
                
                Log.d("RAMBLE_DEEPLINK", "Extracted trip ID: $tripId from path: ${data.path}")
                deepLinkTripId.value = tripId
            }
        }
    }
}

@Composable
private fun MainAppContent(
    onSignOut: () -> Unit,
    initialDeepLinkTripId: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val homeViewModel: HomeViewModel = viewModel()
    var currentScreen by remember { mutableStateOf("home") }
    var previousScreen by remember { mutableStateOf("home") }
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    var selectedTripId by remember { mutableStateOf<String?>(null) }
    var isJoining by remember { mutableStateOf(false) }

    // Handle deep links
    val context = LocalContext.current
    LaunchedEffect(initialDeepLinkTripId) {
        if (initialDeepLinkTripId != null) {
            Log.d("RAMBLE_DEEPLINK", "Started joining flow for ID: $initialDeepLinkTripId")
            isJoining = true
            try {
                val joinedTripId = TripRepository.joinTripById(initialDeepLinkTripId)
                if (joinedTripId != null) {
                    selectedTripId = joinedTripId
                    currentScreen = "trip"
                } else {
                    Toast.makeText(context, "Error joining trip", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("RAMBLE_DEEPLINK", "Error joining trip", e)
                Toast.makeText(context, "Failed to join trip", Toast.LENGTH_SHORT).show()
            } finally {
                isJoining = false
                onDeepLinkHandled()
            }
        }
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
        // Planner Screen: Kept in cache for smoothness
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

        // Middle layer: HomeScreen
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

        // Explore Screen
        val isExploreActive = currentScreen == "explore"
        val exploreAlpha = if (isExploreActive) 1f else 0f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (isExploreActive) 1.15f else 0f)
                .graphicsLayer {
                    alpha = exploreAlpha
                }
        ) {
            ExploreScreen(
                viewModel = homeViewModel,
                onTripClick = { trip ->
                    selectedTripId = trip.id
                    currentScreen = "expanding"
                }
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
            visible = currentScreen == "home" || currentScreen == "planner" || currentScreen == "profile" || currentScreen == "explore",
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

        // Top layer: overlay expands from card bounds
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

        // Global Joining Overlay
        if (isJoining) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Joining trip...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
