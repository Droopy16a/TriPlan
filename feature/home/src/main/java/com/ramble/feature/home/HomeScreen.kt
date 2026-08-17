package com.ramble.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ramble.core.ai.AiPlannerService
import android.Manifest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.graphics.DashPathEffect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramble.core.location.Properties
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.collectAsState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramble.core.ai.SavedTrip
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.ramble.core.designsystem.theme.*
import com.ramble.core.designsystem.util.clickWithDelay
import com.ramble.core.designsystem.util.parseCurrency
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.res.ResourcesCompat

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPlanTripClick: () -> Unit,
    onTripClick: (Rect, String?) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    var isScrollingDown by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val expansionAnimatable = remember { Animatable(0f) }
    val isSearchFormExpanded by viewModel.isSearchFormExpanded.collectAsState()
    val destinationInput by viewModel.destinationQuery.collectAsState()
    val departureInput by viewModel.departureQuery.collectAsState()
    val scope = rememberCoroutineScope()
    val generationState by viewModel.generationState.collectAsState()
    val savedTrips by viewModel.savedTrips.collectAsState()
    val destinationSuggestions by viewModel.destinationSuggestions.collectAsState()
    val departureSuggestions by viewModel.departureSuggestions.collectAsState()
    val isLoading = generationState is TripGenerationState.Loading
    
    // Auto-collapse form and reset state when generation succeeds
    LaunchedEffect(generationState) {
        if (generationState is TripGenerationState.Success) {
            viewModel.setSearchFormExpanded(false)
            viewModel.resetState()
        }
    }
    
    LaunchedEffect(isSearchFormExpanded) {
        if (isSearchFormExpanded) {
            expansionAnimatable.animateTo(1f, tween(300))
        } else {
            expansionAnimatable.animateTo(0f, tween(300))
            isScrollingDown = false
        }
    }

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10) {
                    isScrollingDown = true
                } else if (available.y > 10) {
                    isScrollingDown = false
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Over-scroll at the top (pulling down)
                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    val delta = available.y / 400f
                    scope.launch {
                        expansionAnimatable.snapTo((expansionAnimatable.value + delta).coerceAtMost(1f))
                    }
                }
                return super.onPostScroll(consumed, available, source)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (expansionAnimatable.value >= 0.95f) {
                    // Trigger form expansion
                    viewModel.setSearchFormExpanded(true)
                } else if (expansionAnimatable.value > 0f && !isSearchFormExpanded) {
                    expansionAnimatable.animateTo(0f, tween(300))
                }
                return super.onPreFling(available)
            }
        }
    }

    val context = LocalContext.current
    val barsOffset by animateFloatAsState(
        targetValue = if (isScrollingDown) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    // Vibration when search bar is fully extended
    LaunchedEffect(expansionAnimatable.value >= 1f) {
        if (expansionAnimatable.value >= 1f) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(60)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .nestedScroll(nestedScrollConnection)
    ) {
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topPadding + 110.dp,
                bottom = bottomPadding + 110.dp
            )
        ) {
            item {
                val isEmpty = savedTrips.isEmpty()
                val pageCount = if (isEmpty) 1 else savedTrips.size
                val pagerState = rememberPagerState(pageCount = { pageCount })
                val context = LocalContext.current
                var hasVibratedOnStart by remember { mutableStateOf(false) }
                
                LaunchedEffect(pagerState.currentPage) {
                    if (hasVibratedOnStart) {
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            vibratorManager.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        }
                        
                        if (vibrator.hasVibrator()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(30)
                            }
                        }
                    } else {
                        hasVibratedOnStart = true
                    }
                }

                Column {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        // pageOffset is read in composition because it is forwarded to HeroTripCard.
                        // alpha, scale and absOffset are moved inside graphicsLayer so their
                        // continuous changes during swiping only trigger a cheap draw-phase update
                        // rather than a full recomposition of the card subtree.
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

                        var cardBounds by remember { mutableStateOf(Rect.Zero) }
                        
                        // Pages 0+ = All trips in repository
                        val savedTrip = if (isEmpty) null else savedTrips.getOrNull(page)
                        
                        HeroTripCard(
                            savedTrip = savedTrip,
                            pageOffset = pageOffset,
                            isFocused = pagerState.currentPage == page,
                            modifier = Modifier
                                .graphicsLayer {
                                    val absOffset = pageOffset.absoluteValue
                                    this.alpha = 1f - (0.4f * absOffset.coerceIn(0f, 1f))
                                    this.scaleX = 1f - (0.15f * absOffset.coerceIn(0f, 1f))
                                    this.scaleY = 1f - (0.15f * absOffset.coerceIn(0f, 1f))
                                    
                                    // Dynamic 3D effect
                                    this.rotationZ = pageOffset * 5f // Slight tilt
                                    this.translationY = absOffset * 20f // Float effect
                                    this.cameraDistance = 8f * density
                                }
                                .onGloballyPositioned { coords ->
                                    cardBounds = coords.boundsInWindow()
                                },
                            onClick = { onTripClick(cardBounds, savedTrip?.id) }
                        )
                    }

                    if (!isEmpty) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(pageCount) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isSelected) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) DeepGraphite else Color.Gray.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Popular trips",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                val popularTrips by viewModel.exploreSearchResults.collectAsState()
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val displayTrips = popularTrips.take(4)
                    items(displayTrips.size, key = { displayTrips[it].id }) { index ->
                        PopularTripCard(
                            trip = displayTrips[index],
                            onClick = { onTripClick(androidx.compose.ui.geometry.Rect.Zero, displayTrips[index].id) }
                        )
                    }
                }
            }
        }

        // Scrim — dismisses the form when tapping outside
        if (isSearchFormExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        viewModel.setSearchFormExpanded(false)
                    }
            )
        }

        // Top Search Bar
        SearchBar(
            expansion = if (isSearchFormExpanded) 1f else expansionAnimatable.value,
            isFormExpanded = isSearchFormExpanded,
            onExpand = {
                viewModel.setSearchFormExpanded(true)
            },
            onClose = {
                viewModel.setSearchFormExpanded(false)
                viewModel.updateDepartureSuggestions("")
                viewModel.updateDestinationSuggestions("")
            },
            onPlanTrip = { departure, dest, start, end, trav, budget, pref ->
                viewModel.generateTrip(departure, dest, start, end, trav, budget, pref)
            },
            departure = departureInput,
            onDepartureChange = {
                viewModel.updateDepartureQuery(it)
            },
            departureSuggestions = departureSuggestions,
            destination = destinationInput,
            onDestinationChange = { 
                viewModel.updateDestinationQuery(it) 
            },
            onSuggestionClick = { field, suggestion ->
                if (field == SearchField.Departure) {
                    viewModel.updateDepartureQuery(suggestion.displayName)
                } else {
                    viewModel.updateDestinationQuery(suggestion.displayName)
                }
            },
            onClearDepartureSuggestions = { viewModel.updateDepartureSuggestions("") },
            onClearDestinationSuggestions = { viewModel.updateDestinationSuggestions("") },
            destinationSuggestions = destinationSuggestions,
            startDate = viewModel.startDate.collectAsState().value,
            endDate = viewModel.endDate.collectAsState().value,
            onDateRangeChange = { start, end -> viewModel.updateDateRange(start, end) },
            travelers = viewModel.travelers.collectAsState().value,
            onTravelersChange = { viewModel.updateTravelers(it) },
            budget = viewModel.budget.collectAsState().value,
            onBudgetChange = { viewModel.updateBudget(it) },
            preferences = viewModel.preferences.collectAsState().value,
            onPreferencesChange = { viewModel.updatePreferences(it) },
            isLoading = isLoading,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { 
                    val yOffset = if (isSearchFormExpanded) 0 else -(barsOffset * 300).roundToInt()
                    IntOffset(0, yOffset) 
                }
                .statusBarsPadding()
                .padding(top = 8.dp)
                .imePadding()
        )

        // Error snackbar
        val errorState = generationState as? TripGenerationState.Error
        if (errorState != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 90.dp, start = 24.dp, end = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFF6B6B)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "AI error: ${errorState.message.take(60)}",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.resetState() }) {
                            Text("Dismiss", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

enum class SearchField { None, Departure, Destination, Travelers, Budget }

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    expansion: Float = 0f,
    isFormExpanded: Boolean = false,
    onExpand: () -> Unit = {},
    onClose: () -> Unit = {},
    onPlanTrip: (String, String, LocalDate?, LocalDate?, String, String, String) -> Unit = { _,_,_,_,_,_,_ -> },
    isLoading: Boolean = false,
    departureSuggestions: List<Properties> = emptyList(),
    destinationSuggestions: List<Properties> = emptyList(),
    departure: String = "",
    onDepartureChange: (String) -> Unit = {},
    destination: String = "",
    onDestinationChange: (String) -> Unit = {},
    onSuggestionClick: (SearchField, Properties) -> Unit = { _, _ -> },
    onClearDepartureSuggestions: () -> Unit = {},
    onClearDestinationSuggestions: () -> Unit = {},
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit = { _, _ -> },
    travelers: String = "",
    onTravelersChange: (String) -> Unit = {},
    budget: String = "",
    onBudgetChange: (String) -> Unit = {},
    preferences: String = "",
    onPreferencesChange: (String) -> Unit = {}
) {
    val extraHeight = (expansion * 24).dp
    val horizontalPadding = (16 + 16 * (1f - expansion)).dp  // 32dp collapsed → 16dp expanded
    val cornerRadius = (50 * (1f - (expansion * 0.6f))).dp

    val elevationPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        (22 + (expansion * 8)).dp.toPx()
    }
    val cornerRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        cornerRadius.toPx()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .graphicsLayer {
                this.shadowElevation = elevationPx
                this.shape = RoundedCornerShape(cornerRadiusPx)
                this.clip = false
                this.spotShadowColor = Color(0x80000000)
                this.ambientShadowColor = Color(0x3C000000)
            },
        shape = RoundedCornerShape(cornerRadius),
        color = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(cornerRadius)
                )
        ) {
            AnimatedContent(
                targetState = isFormExpanded,
                label = "search_form_transition"
            ) { isExpanded ->
                if (isExpanded) {
                    // Form-level state
                    var showDatePicker by remember { mutableStateOf(false) }
                    
                    val profile by com.ramble.core.ai.ProfileRepository.profile.collectAsState()
                    var showAdvancedSettings by remember { mutableStateOf(false) }
                    var travelStyle by remember { mutableStateOf(profile.travelStyle) }
                    var interests by remember { mutableStateOf(profile.interests) }
                    var accommodation by remember { mutableStateOf(profile.accommodationPreference) }
                    var transportation by remember { mutableStateOf(profile.transportationPreference) }
                    var food by remember { mutableStateOf(profile.foodPreferences) }

                    var focusedField by remember { mutableStateOf(SearchField.None) }
                    val departureInteractionSource = remember { MutableInteractionSource() }
                    val destinationInteractionSource = remember { MutableInteractionSource() }
                    val travelersInteractionSource = remember { MutableInteractionSource() }
                    val budgetInteractionSource = remember { MutableInteractionSource() }
                    val isDepartureFocused by departureInteractionSource.collectIsFocusedAsState()
                    val isDestinationFocused by destinationInteractionSource.collectIsFocusedAsState()
                    val isTravelersFocused by travelersInteractionSource.collectIsFocusedAsState()
                    val isBudgetFocused by budgetInteractionSource.collectIsFocusedAsState()

                    LaunchedEffect(isDepartureFocused, isDestinationFocused, isTravelersFocused, isBudgetFocused) {
                        focusedField = when {
                            isDepartureFocused -> SearchField.Departure
                            isDestinationFocused -> SearchField.Destination
                            isTravelersFocused -> SearchField.Travelers
                            isBudgetFocused -> SearchField.Budget
                            else -> SearchField.None
                        }
                        
                        // When switching focus, clear the suggestions of the field that lost focus
                        if (isDepartureFocused) {
                            onClearDestinationSuggestions()
                        }
                        if (isDestinationFocused) {
                            onClearDepartureSuggestions()
                        }
                    }

                    val departureWeight by animateFloatAsState(
                        targetValue = when (focusedField) {
                            SearchField.Departure -> 0.85f
                            SearchField.Destination -> 0.15f
                            else -> 0.5f
                        },
                        label = "departure_weight"
                    )
                    val destinationWeight by animateFloatAsState(
                        targetValue = when (focusedField) {
                            SearchField.Destination -> 0.85f
                            SearchField.Departure -> 0.15f
                            else -> 0.5f
                        },
                        label = "destination_weight"
                    )
                    val travelersWeight by animateFloatAsState(
                        targetValue = when (focusedField) {
                            SearchField.Travelers -> 0.65f
                            SearchField.Budget -> 0.35f
                            else -> 0.5f
                        },
                        label = "travelers_weight"
                    )
                    val budgetWeight by animateFloatAsState(
                        targetValue = when (focusedField) {
                            SearchField.Budget -> 0.65f
                            SearchField.Travelers -> 0.35f
                            else -> 0.5f
                        },
                        label = "budget_weight"
                    )
                    
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val geocodingProvider = remember(context) { AiPlannerService(context).geocodingProvider }
                    
                    val locationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        if (permissions.values.any { it }) {
                            // Location granted, try to get location
                            try {
                                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                                val location = locationManager.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)
                                    ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                                    ?: locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                    
                                if (location != null) {
                                    scope.launch {
                                        val props = geocodingProvider.reverseGeocodeToProperties(location.latitude, location.longitude)
                                        if (props != null) {
                                            onDepartureChange(props.displayName)
                                        } else {
                                            onDepartureChange("${location.latitude}, ${location.longitude}")
                                        }
                                    }
                                }
                            } catch (e: SecurityException) {
                                // Ignored
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        // Request location on form open to pre-fill departure
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }

                    val dateRangePickerState = rememberDateRangePickerState(
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                return utcTimeMillis >= today
                            }
                        }
                    )

                    val datesText = if (startDate != null && endDate != null) {
                        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                        "${startDate!!.format(formatter)} – ${endDate!!.format(formatter)}"
                    } else ""

                    if (showDatePicker) {
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val start = dateRangePickerState.selectedStartDateMillis
                                        val end = dateRangePickerState.selectedEndDateMillis
                                        if (start != null && end != null) {
                                            val s = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                                            val e = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalDate()
                                            onDateRangeChange(s, e)
                                        }
                                        showDatePicker = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = BrandDarkGreen)
                                ) {
                                    Text("OK")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDatePicker = false },
                                    colors = ButtonDefaults.textButtonColors(contentColor = DeepGraphite.copy(alpha = 0.6f))
                                ) {
                                    Text("Cancel")
                                }
                            },
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            DateRangePicker(
                                state = dateRangePickerState,
                                title = { 
                                    Text(
                                        text = "Select trip dates", 
                                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = DeepGraphite.copy(alpha = 0.6f)
                                    ) 
                                },
                                headline = {
                                    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
                                    val startText = dateRangePickerState.selectedStartDateMillis?.let {
                                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
                                    } ?: "Start date"
                                    
                                    val endText = dateRangePickerState.selectedEndDateMillis?.let {
                                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
                                    } ?: "End date"

                                    Text(
                                        text = "$startText – $endText",
                                        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                colors = DatePickerDefaults.colors(
                                    selectedDayContainerColor = DeepGraphite,
                                    dayInSelectionRangeContainerColor = MintGreen.copy(alpha = 0.15f),
                                    selectedDayContentColor = Color.White,
                                    todayDateBorderColor = DeepGraphite,
                                    todayContentColor = DeepGraphite
                                ),
                                modifier = Modifier.weight(1f),
                                showModeToggle = false
                            )
                        }
                    }

                    val formColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = DeepGraphite,
                        focusedLabelColor    = DeepGraphite,
                        cursorColor          = DeepGraphite,
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        unfocusedLabelColor  = Color(0xFF888888)
                    )

                    val scrollState = rememberScrollState()
                    LaunchedEffect(isExpanded) {
                        if (isExpanded) {
                            scrollState.scrollTo(0)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((LocalConfiguration.current.screenHeightDp * 0.82f).dp)
                            .padding(24.dp)
                    ) {
                        // Header (Fixed)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Plan a Trip",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepGraphite
                                )
                                Text(
                                    "Let AI build your perfect itinerary",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepGraphite.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = DeepGraphite.copy(alpha = 0.6f))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Scrollable Form Content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Departure
                                OutlinedTextField(
                                    value = departure,
                                    onValueChange = { onDepartureChange(it) },
                                    label = { 
                                        if (departureWeight > 0.4f) {
                                            Text(
                                                text = "Departure",
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    placeholder = { 
                                        Text(
                                            text = "Where are you starting from?",
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        ) 
                                    },
                                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                    modifier = Modifier
                                        .weight(departureWeight),
                                    interactionSource = departureInteractionSource,
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = formColors
                                )

                                // Destination
                                OutlinedTextField(
                                    value = destination,
                                    onValueChange = {
                                        onDestinationChange(it)
                                    },
                                    label = { 
                                        if (destinationWeight > 0.4f) {
                                            Text(
                                                text = "Destination",
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    placeholder = { 
                                        Text(
                                            text = "Tokyo, Bali, Paris…",
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        ) 
                                    },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    modifier = Modifier
                                        .weight(destinationWeight),
                                    interactionSource = destinationInteractionSource,
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = formColors
                                )
                            }

                            // Suggestions
                            val currentSuggestions = when (focusedField) {
                                SearchField.Departure -> departureSuggestions
                                SearchField.Destination -> destinationSuggestions
                                else -> emptyList()
                            }
                            val currentQuery = if (focusedField == SearchField.Departure) departure else destination

                            if (currentSuggestions.isNotEmpty() && currentQuery.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                                ) {
                                    Column {
                                        currentSuggestions.forEach { suggestion ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onSuggestionClick(focusedField, suggestion)
                                                    }
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn, 
                                                    contentDescription = null, 
                                                    modifier = Modifier.size(18.dp), 
                                                    tint = DeepGraphite.copy(alpha = 0.5f)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    suggestion.displayName, 
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = DeepGraphite
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = datesText,
                                onValueChange = { },
                                label = { Text("Dates") },
                                placeholder = { Text("Select dates") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = DeepGraphite,
                                    disabledBorderColor = Color(0xFFDDDDDD),
                                    disabledLabelColor = Color(0xFF888888),
                                    disabledLeadingIconColor = DeepGraphite.copy(alpha = 0.6f),
                                    disabledPlaceholderColor = Color(0xFF888888)
                                )
                            )
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = travelers,
                                    onValueChange = { onTravelersChange(it) },
                                    label = {
                                        if (travelersWeight > 0.4f) {
                                            Text(
                                                text = "Travelers",
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    placeholder = { Text("2") },
                                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                    modifier = Modifier.weight(travelersWeight),
                                    interactionSource = travelersInteractionSource,
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = formColors
                                )

                                OutlinedTextField(
                                    value = budget,
                                    onValueChange = { onBudgetChange(it) },
                                    label = {
                                        if (budgetWeight > 0.4f) {
                                            Text(
                                                text = "Budget",
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    placeholder = { Text("e.g. \$2000") },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                                    modifier = Modifier.weight(budgetWeight),
                                    interactionSource = budgetInteractionSource,
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = formColors
                                )
                            }

                            // Preferences
                            OutlinedTextField(
                                value = preferences,
                                onValueChange = { onPreferencesChange(it) },
                                label = { Text("Preferences") },
                                placeholder = { Text("e.g. Vegetarian, Museums, Hiking…") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Done
                                ),
                                colors = formColors
                            )
                            
                            // Advanced Settings
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAdvancedSettings = !showAdvancedSettings }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Advanced Settings",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepGraphite
                                )
                                Icon(
                                    if (showAdvancedSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = DeepGraphite
                                )
                            }
                            
                            AnimatedVisibility(
                                visible = showAdvancedSettings,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    AdvancedSettingSingleSelector(
                                        label = "Travel Style",
                                        options = listOf("Budget", "Balanced", "Comfort", "Luxury"),
                                        selectedOption = travelStyle,
                                        onOptionSelected = { travelStyle = it }
                                    )
                                    AdvancedSettingSelector(
                                        label = "Interests",
                                        options = listOf("🏛️ Culture", "🍜 Food", "🏖️ Beaches", "🥾 Nature", "🎨 Art", "🌃 Nightlife", "🛍️ Shopping", "⚽ Sports", "📸 Photography"),
                                        selectedOptions = interests,
                                        onOptionToggled = { 
                                            interests = if (interests.contains(it)) interests - it else interests + it
                                        },
                                        optionToValue = { it.substringAfter(" ").trim() }
                                    )
                                    AdvancedSettingSelector(
                                        label = "Accommodation",
                                        options = listOf("Hotel", "Hostel", "Apartment", "Guest house", "Camping"),
                                        selectedOptions = accommodation,
                                        onOptionToggled = {
                                            accommodation = if (accommodation.contains(it)) accommodation - it else accommodation + it
                                        }
                                    )
                                    AdvancedSettingSelector(
                                        label = "Transportation",
                                        options = listOf("Walking", "Public transport", "Car", "Taxi", "Bike"),
                                        selectedOptions = transportation,
                                        onOptionToggled = {
                                            transportation = if (transportation.contains(it)) transportation - it else transportation + it
                                        }
                                    )
                                    AdvancedSettingSelector(
                                        label = "Food Preferences",
                                        options = listOf("Vegetarian", "Vegan", "Halal", "Gluten-free"),
                                        selectedOptions = food,
                                        onOptionToggled = {
                                            food = if (food.contains(it)) food - it else food + it
                                        }
                                    )
                                }
                            }
                            
                            // Extra space at bottom to ensure fields aren't cut off by keyboard
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Submit Button (Fixed)
                        Button(
                            onClick = { 
                                if (!isLoading) {
                                    val advancedPrefs = listOf(
                                        if (travelStyle.isNotBlank()) "Travel Style: $travelStyle" else null,
                                        if (interests.isNotEmpty()) "Interests: ${interests.joinToString(", ")}" else null,
                                        if (accommodation.isNotEmpty()) "Accommodation: ${accommodation.joinToString(", ")}" else null,
                                        if (transportation.isNotEmpty()) "Transportation: ${transportation.joinToString(", ")}" else null,
                                        if (food.isNotEmpty()) "Food: ${food.joinToString(", ")}" else null
                                    ).filterNotNull().joinToString("; ")

                                    val finalPreferences = if (advancedPrefs.isNotBlank()) {
                                        if (preferences.isNotBlank()) "$preferences\n$advancedPrefs" else advancedPrefs
                                    } else {
                                        preferences
                                    }

                                    onPlanTrip(departure, destination, startDate, endDate, travelers, budget, finalPreferences) 
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepGraphite
                            )
                        ) {
                            if (isLoading) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFFFD700) // gold shimmer on dark bg
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Plan my trip",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp + extraHeight)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onExpand() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (expansion > 0.5f) Arrangement.Start else Arrangement.Center
                    ) {
                        if (expansion > 0.5f) {
                            Spacer(modifier = Modifier.width(24.dp))
                        }
                        Icon(Icons.Default.Search, contentDescription = null, tint = DeepGraphite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (expansion > 0.7f) "Search destination..." else "What's next ?",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (expansion > 0.7f) DeepGraphite.copy(alpha = 0.5f) else DeepGraphite,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavPill(
    currentScreen: String,
    onTabClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by com.ramble.core.ai.ProfileRepository.profile.collectAsState()

    val indicatorOffset by animateDpAsState(
        targetValue = when (currentScreen) {
            "planner" -> (-33).dp
            "explore" -> 33.dp
            "profile" -> 99.dp
            else -> (-99).dp // Defaults to "home" for "home", "expanding", or others
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorOffset"
    )

    Surface(
        modifier = modifier
            .width(280.dp)
            .height(64.dp)
            .shadow(12.dp, RoundedCornerShape(50))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            ),
        shape = RoundedCornerShape(50),
        color = DeepGraphite.copy(alpha = 0.9f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Sliding Indicator
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .size(width = 60.dp, height = 40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            )

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavIcon(Icons.Default.Home, "Home", isSelected = currentScreen == "home", onClick = { onTabClick("home") })
                NavIcon(Icons.Default.FlightTakeoff, "Planner", isSelected = currentScreen == "planner", onClick = { onTabClick("planner") })
                NavIcon(Icons.Default.Map, "Explore", isSelected = currentScreen == "explore", onClick = { onTabClick("explore") })
                NavIcon(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    isSelected = currentScreen == "profile",
                    avatarUrl = profile.avatarUrl,
                    onClick = { onTabClick("profile") }
                )
            }
        }
    }
}

@Composable
fun NavIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    avatarUrl: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                vibrateDevice(context, 30)
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (avatarUrl != null && label == "Profile") {
            AsyncImage(
                model = avatarUrl,
                contentDescription = label,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun HeroTripCard(
    modifier: Modifier = Modifier,
    savedTrip: SavedTrip? = null,
    pageOffset: Float = 0f,
    isFocused: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (savedTrip == null) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(440.dp)
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            color = UIBackgroundGray,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = DeepGraphite.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Scroll up to start a new adventure",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepGraphite.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
        }
        return
    }

    // Dynamic budget calculation
    val totalBudget = parseCurrency(savedTrip.budget).let {
        if (it == 0.0) 2500.0 else it // Fallback for static card
    }
    val spentBudget = remember(savedTrip) {
        val itineraryCost = savedTrip.itinerary?.days?.flatMap { it.steps }?.sumOf {
            it.estimatedCost ?: 0.0
        } ?: 0.0
        val manualExpenses = savedTrip.expenses?.sumOf { it.amount } ?: 0.0

        itineraryCost + manualExpenses
    }

    val remainingBudget = (totalBudget - spentBudget).coerceAtLeast(0.0)
    val targetProgress = if (totalBudget > 0) (spentBudget / totalBudget).toFloat().coerceIn(0f, 1f) else 0f

    // Animate budget progress when focused
    val budgetProgress by animateFloatAsState(
        targetValue = if (isFocused) targetProgress else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "budgetProgress"
    )

    // Display values
    val displayEmoji  = savedTrip.emoji
    val cityName = savedTrip.destination.substringBefore(",").trim()
    val displayTitle = if (savedTrip.title.isNotBlank()) savedTrip.title else {
        val year = savedTrip.itinerary?.days?.firstOrNull()?.date?.substringBefore("-") ?: "2026"
        "$cityName $year"
    }
    val displayDates  = savedTrip.dates
    val isAiGenerated = true

    val daysLeft = remember(savedTrip) {
        val startDate = savedTrip.itinerary?.days?.firstOrNull()?.date?.let {
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        } ?: LocalDate.of(2026, 10, 12)
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), startDate)
    }

    val daysLeftText = when {
        daysLeft > 1 -> "$daysLeft Days Left"
        daysLeft == 1L -> "1 Day Left"
        daysLeft == 0L -> "Starts Today"
        else -> null // Trip in progress or past
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.06f),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 0.dp,
        onClick = {
            vibrateDevice(context, 30)
            clickWithDelay(scope, onClick = onClick)
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Map Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFFF0F0F0))
            ) {
                var isMapVisible by remember { mutableStateOf(false) }

                key(savedTrip?.id) {
                    AndroidView(
                        factory = { ctx ->
                            MapLibre.getInstance(ctx)
                            val options = MapLibreMapOptions.createFromAttributes(ctx, null)
                                .textureMode(true)

                            MapView(ctx, options).apply {
                                getMapAsync { map ->
                                    map.setMaxZoomPreference(16.0)
                                    map.uiSettings.isCompassEnabled = false
                                    map.uiSettings.isLogoEnabled = false
                                    map.uiSettings.isAttributionEnabled = false
                                    map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                                        val itinerary = savedTrip?.itinerary
                                        val validSteps = itinerary?.days?.flatMap { it.steps }
                                            ?.filter { it.lat != null && it.lon != null }
                                            ?: emptyList()

                                        val routePoints = if (validSteps.isNotEmpty()) {
                                            validSteps.map { LatLng(it.lat!!, it.lon!!) to it.title }
                                        } else {
                                            listOf(
                                                LatLng(49.0097, 2.5479) to "Paris CDG",
                                                LatLng(34.4320, 135.2304) to "Osaka KIX",
                                                LatLng(35.0116, 135.7681) to "Kyoto"
                                            )
                                        }

                                        // Routes - solid black line as seen in image
                                        if (routePoints.size > 1) {
                                            val lineString = LineString.fromLngLats(routePoints.map { Point.fromLngLat(it.first.longitude, it.first.latitude) })
                                            
                                            style.addSource(GeoJsonSource("routes-source", Feature.fromGeometry(lineString)))
                                            style.addLayer(LineLayer("routes-layer", "routes-source").withProperties(
                                                PropertyFactory.lineColor("#000000"),
                                                PropertyFactory.lineWidth(3f),
                                                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                                            ))
                                        }

                                        // Markers - showing first and last for mini map to keep it clean, or all if few
                                        val markersToShow = if (routePoints.size <= 3) routePoints else listOf(routePoints.first(), routePoints.last())

                                        markersToShow.forEachIndexed { index, (pos, label) ->
                                            val markerBitmap = createMiniMapMarkerBitmap(ctx, label)
                                            val markerId = "marker-$index"
                                            style.addImage(markerId, markerBitmap)

                                            val sourceId = "marker-source-$index"
                                            style.addSource(GeoJsonSource(sourceId, Feature.fromGeometry(
                                                Point.fromLngLat(pos.longitude, pos.latitude)
                                            )))

                                            style.addLayer(SymbolLayer("marker-layer-$index", sourceId).withProperties(
                                                PropertyFactory.iconImage(markerId),
                                                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                                                PropertyFactory.iconOffset(arrayOf(0f, 4f)),
                                                PropertyFactory.iconAllowOverlap(true)
                                            ))
                                        }

                                        if (routePoints.isNotEmpty()) {
                                            val routeBounds = LatLngBounds.Builder().apply {
                                                routePoints.forEach { include(it.first) }
                                            }.build()

                                            try {
                                                if (routePoints.size > 1) {
                                                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(routeBounds, 60))
                                                } else {
                                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(routePoints.first().first, 14.0))
                                                }
                                            } catch (e: Exception) {
                                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(routePoints.first().first, 14.0))
                                            }
                                        }
                                        isMapVisible = true
                                    }
                                }
                            }
                        },
                        update = { _ -> },
                        onRelease = { view -> view.onDestroy() },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = -pageOffset * 100f
                            }
                    )
                }

                if (!isMapVisible) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
                }

                // Transparent overlay for click
                Box(modifier = Modifier.fillMaxSize().clickable {
                    vibrateDevice(context, 30)
                    clickWithDelay(scope, onClick = onClick)
                })

                // Top-right badge
                if (daysLeftText != null) {
                    Surface(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd),
                        color = Color.White,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            daysLeftText,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon Box
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DeepGraphite),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(displayEmoji ?: "✈️", style = MaterialTheme.typography.titleLarge)
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column {
                        Text(
                            displayTitle, 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            displayDates ?: "", 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (isAiGenerated) {
                            Text(
                                "✦ ${savedTrip.travelers} travelers · ${savedTrip.budget}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        } else {
                            OverlappingAvatars(initials = listOf("A", "B", "C"), extra = "+1")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Budget Section
                val remainingFormatted = String.format(Locale.US, "%,.0f", remainingBudget).replace(",", " ")
                Text(
                    "$ $remainingFormatted remaining", 
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Progress Bar with Dot
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(UIBorderGray)
                ) {
                    val progressWidth = maxWidth * budgetProgress
                    
                    // Progress Fill
                    Box(
                        modifier = Modifier
                            .width(progressWidth)
                            .fillMaxHeight()
                            .background(BrandLightGreen)
                    )
                    
                    // White Dot
                    if (budgetProgress > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = progressWidth - 5.dp)
                                .align(Alignment.CenterStart)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PopularTripCard(
    trip: com.ramble.core.ai.SavedTrip? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val title = if (trip?.title?.isNotBlank() == true) trip.title else (trip?.destination ?: "Paris, France")
    val budget = trip?.budget ?: "$ 1 500 total"
    val emoji = trip?.emoji ?: "🗼"
    Column(modifier = modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SkyBlueLight),
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = trip?.imageUrl
            if (imageUrl != null) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(emoji, fontSize = 64.sp)
            }
            
            var isLiked by remember { mutableStateOf(false) }
            IconButton(
                onClick = { isLiked = !isLiked },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title, 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.SemiBold, 
            maxLines = 1, 
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp).width(160.dp)
        )
        Text(
            text = budget, 
            style = MaterialTheme.typography.bodyMedium, 
            color = Color.Gray, 
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
fun OverlappingAvatars(
    initials: List<String>,
    extra: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-12).dp)
    ) {
        initials.forEach { initial ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DeepGraphite)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(DeepGraphite)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = extra,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

fun createMiniMapMarkerBitmap(context: android.content.Context, text: String): Bitmap {
    val typeface = ResourcesCompat.getFont(context, com.ramble.core.designsystem.R.font.poppins_semibold)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#2B2D42")
        textSize = 40f
        this.typeface = typeface
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 1f, android.graphics.Color.parseColor("#40000000"))
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#2B2D42")
    }
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }

    val bounds = android.graphics.Rect()
    textPaint.getTextBounds(text, 0, text.length, bounds)
    val width = bounds.width() + 80
    val height = bounds.height() + 80

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = width / 2f
    val cy = height - 18f

    canvas.drawCircle(cx, cy, 14f, haloPaint)
    canvas.drawCircle(cx, cy, 9f, dotPaint)
    canvas.drawText(text, cx, cy - 28f, textPaint)

    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryBottomSheetContent(
    itinerary: com.ramble.core.ai.TripItinerary,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val categoryColors = mapOf(
        "Food" to Color(0xFFFF6B35),
        "Activity" to Color(0xFF4ECDC4),
        "Transport" to Color(0xFF45B7D1),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "AI Generated",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    itinerary.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    itinerary.destination,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Summary
        Text(
            itinerary.summary,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(20.dp))

        // Budget allocation
        Text("Budget Allocation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                "🏨" to itinerary.budgetAllocation.accommodation,
                "🍽️" to itinerary.budgetAllocation.food,
                "🚌" to itinerary.budgetAllocation.transport,
                "🎭" to itinerary.budgetAllocation.activities
            ).forEach { (emoji, pct) ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(emoji, fontSize = 18.sp)
                        Text(pct, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Days
        itinerary.days.forEach { day ->
            // Day header
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF6C63FF)
                        ) {
                            Text(
                                "Day ${day.dayNumber}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        val dateText = try {
                            val date = LocalDate.parse(day.date)
                            date.format(DateTimeFormatter.ofPattern("MMM d, E", Locale.getDefault()))
                        } catch (_: Exception) {
                            day.date
                        }
                        Text(text = dateText, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Default.WbSunny, contentDescription = "Sunny", tint = Color(0xFFFDB813), modifier = Modifier.size(16.dp))
                        Text(text = "22°C", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(day.theme, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))

            // Steps
            day.steps.forEach { step ->
                val color = categoryColors[step.category] ?: Color(0xFF6C63FF)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    // Timeline dot + line
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .width(1.dp)
                                .height(50.dp)
                        ) {
                            drawLine(
                                color = color.copy(alpha = 0.4f),
                                start = Offset(x = size.width / 2, y = 0f),
                                end = Offset(x = size.width / 2, y = size.height),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(step.time, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            if (step.estimatedCost != null) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = color.copy(alpha = 0.18f)
                                ) {
                                    val costFormatted = String.format(Locale.US, "$ %.0f", step.estimatedCost)
                                    Text(
                                        costFormatted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = color,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Text(
                            step.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            step.description,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        if (step.lat != null && step.lon != null) {
                            val context = LocalContext.current
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    val uri = "geo:${step.lat},${step.lon}?q=${step.lat},${step.lon}(${step.title})"
                                    val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
                                    context.startActivity(intent)
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    "See more",
                                    fontSize = 12.sp,
                                    color = color,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = color
                                )
                            }
                        }
                    }
                }
            }
            
            // See Itinerary Button for each day
            val validSteps = day.steps.filter { it.lat != null && it.lon != null }
            if (validSteps.size >= 2) {
                val context = LocalContext.current
                Button(
                    onClick = {
                        val origin = "${validSteps.first().lat},${validSteps.first().lon}"
                        val destination = "${validSteps.last().lat},${validSteps.last().lon}"
                        val waypoints = if (validSteps.size > 2) {
                            validSteps.subList(1, validSteps.size - 1)
                                .joinToString("|") { "${it.lat},${it.lon}" }
                        } else null

                        val uri = StringBuilder("https://www.google.com/maps/dir/?api=1")
                            .append("&origin=$origin")
                            .append("&destination=$destination")
                        
                        if (waypoints != null) {
                            uri.append("&waypoints=$waypoints")
                        }

                        val intent = Intent(Intent.ACTION_VIEW, uri.toString().toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("See Itinerary in Maps", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Save to Trip button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
        ) {
            Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text("Save to my Trips", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

fun vibrateDevice(context: Context, duration: Long = 30) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}

@Composable
fun AdvancedSettingSingleSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    optionToValue: (String) -> String = { it }
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(options.size) { index ->
                val option = options[index]
                val value = optionToValue(option)
                val isSelected = selectedOption == value || selectedOption == option
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DeepGraphite else Color(0xFFF0F0F0))
                        .border(
                            width = 2.dp,
                            color = if (isSelected) DeepGraphite else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onOptionSelected(value) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(option, fontSize = 14.sp, color = if (isSelected) Color.White else DeepGraphite)
                }
            }
        }
    }
}

@Composable
fun AdvancedSettingSelector(
    label: String,
    options: List<String>,
    selectedOptions: List<String>,
    onOptionToggled: (String) -> Unit,
    optionToValue: (String) -> String = { it }
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(options.size) { index ->
                val option = options[index]
                val value = optionToValue(option)
                val isSelected = selectedOptions.contains(value) || selectedOptions.contains(option)
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DeepGraphite else Color(0xFFF0F0F0))
                        .border(
                            width = 2.dp,
                            color = if (isSelected) DeepGraphite else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onOptionToggled(value) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(option, fontSize = 14.sp, color = if (isSelected) Color.White else DeepGraphite)
                }
            }
        }
    }
}
