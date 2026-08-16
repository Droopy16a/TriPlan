package com.ramble.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ramble.core.designsystem.theme.DeepGraphite
import com.ramble.core.designsystem.theme.OffWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.geojson.Point
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.toArgb
import com.ramble.core.location.Properties
import com.ramble.core.ai.AiPlannerService
import androidx.activity.compose.BackHandler

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.clickable

@Composable
fun PlannerScreen(
    isActive: Boolean = true,
    onBack: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    val selectedCity by viewModel.selectedCityProperties.collectAsState()
    val destinationInput by viewModel.destinationQuery.collectAsState()
    val departureInput by viewModel.departureQuery.collectAsState()
    val isSearchFormExpanded by viewModel.isSearchFormExpanded.collectAsState()
    
    // Tap animation state
    var tapPoint by remember { mutableStateOf<Offset?>(null) }
    var tapTrigger by remember { mutableIntStateOf(0) }

    // Search form state
    val expansionAnimatable = remember { Animatable(0f) }
    val generationState by viewModel.generationState.collectAsState()
    val destinationSuggestions by viewModel.destinationSuggestions.collectAsState()
    val departureSuggestions by viewModel.departureSuggestions.collectAsState()
    val isLoading = generationState is TripGenerationState.Loading
    val geocodingProvider = remember(context) { AiPlannerService(context).geocodingProvider }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            mapInstance?.let { map ->
                map.style?.let { style ->
                    enableLocation(context, map, style)
                }
            }
        }
    }
    
    var isMapSpinning by remember { mutableStateOf(true) }
    
    // Auto-collapse form and reset state when generation succeeds
    LaunchedEffect(generationState) {
        if (generationState is TripGenerationState.Success) {
            viewModel.setSearchFormExpanded(false)
            viewModel.resetState()
        }
    }

    LaunchedEffect(isSearchFormExpanded) {
        if (isSearchFormExpanded) {
            isMapSpinning = false
            expansionAnimatable.animateTo(1f, tween(300))
        } else {
            expansionAnimatable.animateTo(0f, tween(300))
        }
    }

    LaunchedEffect(selectedCity) {
        val city = selectedCity
        if (city != null) {
            // Wait a brief moment so the tap feedback is seen before the form expands
            delay(125)

            if (!isSearchFormExpanded) {
                viewModel.setSearchFormExpanded(true)
            }
        }
    }
    
    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }

    LaunchedEffect(isMapSpinning, mapInstance) {
        if (isMapSpinning && mapInstance != null) {
            // Slow auto-rotation loop
            while(isMapSpinning) {
                delay(50)
                mapInstance?.let { map ->
                    val currentPos = map.cameraPosition
                    val target = currentPos.target
                    if (target != null) {
                        val newLng = (target.longitude + 0.1) % 360
                        map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(target.latitude, newLng)))
                    }
                }
            }
        }
    }

    BackHandler(enabled = isActive) {
        if (isSearchFormExpanded) {
            viewModel.setSearchFormExpanded(false)
        } else {
            onBack()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(OffWhite)) {
        // Map as the "Globe"
        AndroidView(
            factory = { ctx ->
                MapLibre.getInstance(ctx)
                val options = MapLibreMapOptions.createFromAttributes(ctx, null)
                    .textureMode(true)
                
                MapView(ctx, options).apply {
                    getMapAsync { map ->
                        mapInstance = map
                        map.uiSettings.isCompassEnabled = false
                        map.uiSettings.isLogoEnabled = false
                        map.uiSettings.isAttributionEnabled = false

                        map.addOnMoveListener(object : MapLibreMap.OnMoveListener {
                            override fun onMoveBegin(detector: MoveGestureDetector) {
                                isMapSpinning = false
                            }
                            override fun onMove(detector: MoveGestureDetector) {}
                            override fun onMoveEnd(detector: MoveGestureDetector) {}
                        })

                        map.addOnMapClickListener { latLng ->
                            isMapSpinning = false
                            
                            val pixel = map.projection.toScreenLocation(latLng)
                            // Trigger finger animation
                            tapPoint = Offset(pixel.x, pixel.y)
                            tapTrigger++

                            // Box for hit testing
                            val rect = android.graphics.RectF(pixel.x - 40, pixel.y - 40, pixel.x + 40, pixel.y + 40)
                            val features = map.queryRenderedFeatures(rect)
                            
                            val cityFeature = features.firstOrNull { f ->
                                val name = f.getStringProperty("name")
                                val cls = f.getStringProperty("class") ?: ""
                                val type = f.getStringProperty("type") ?: ""
                                val isPoint = f.geometry() is Point
                                
                                // Specifically filter for place-related Point features.
                                // This excludes rivers (LineStrings), parks (Polygons), and other non-city data.
                                name != null && isPoint && (
                                    cls == "city" || cls == "town" || cls == "village" || cls == "hamlet" || cls == "suburb" ||
                                    type == "city" || type == "town" || type == "village" ||
                                    cls == "place" || type == "place"
                                )
                            }
                            
                            if (cityFeature != null) {
                                val name = cityFeature.getStringProperty("name")
                                val displayName = cityFeature.getStringProperty("name:en") ?: name
                                val country = cityFeature.getStringProperty("country") 
                                    ?: cityFeature.getStringProperty("adm0_name")
                                    ?: cityFeature.getStringProperty("country_name")
                                val state = cityFeature.getStringProperty("state")
                                    ?: cityFeature.getStringProperty("region")
                                    ?: cityFeature.getStringProperty("adm1_name")

                                val id = cityFeature.id() ?: displayName ?: "unknown"
                                val point = cityFeature.geometry() as? Point
                                
                                val props = Properties(
                                    name = displayName,
                                    city = displayName,
                                    state = state,
                                    country = country,
                                    osm_id = id,
                                    lat = point?.latitude(),
                                    lon = point?.longitude()
                                )
                                viewModel.selectCity(props)

                                if (country.isNullOrBlank() && point != null) {
                                    scope.launch {
                                        val enrichedProps = geocodingProvider.reverseGeocodeToProperties(
                                            point.latitude(), point.longitude()
                                        )
                                        if (enrichedProps != null) {
                                            viewModel.selectCity(enrichedProps)
                                        }
                                    }
                                }
                                true
                            } else {
                                viewModel.clearSelection()
                                false
                            }
                        }
                        
                        map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                            if (ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                enableLocation(ctx, map, style)
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                        
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(20.0, 0.0))
                            .zoom(1.0)
                            .build()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dot wave animation overlay
        tapPoint?.let { point ->
            key(tapTrigger) {
                DotWaveAnimation(
                    point = point,
                    onAnimationEnd = { tapPoint = null }
                )
            }
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .padding(bottom = 70.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            AnimatedVisibility(
                visible = isVisible && !isSearchFormExpanded,
                enter = fadeIn(tween(800, delayMillis = 400)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                ),
                exit = fadeOut()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = DeepGraphite.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Plan your next adventure",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap anywhere on the globe to start exploring or select a destination.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (isSearchFormExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        viewModel.setSearchFormExpanded(false)
                    }
            )
        }

        SearchBar(
            expansion = if (isSearchFormExpanded) 1f else expansionAnimatable.value,
            isFormExpanded = isSearchFormExpanded,
            onExpand = {
                viewModel.setSearchFormExpanded(true)
            },
            onClose = {
                viewModel.setSearchFormExpanded(false)
                viewModel.clearSelection()
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
                if (selectedCity != null && it != selectedCity?.displayName) {
                    viewModel.clearSelection()
                }
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
                .statusBarsPadding()
                .padding(top = 8.dp)
                .imePadding()
        )
    }
}

@android.annotation.SuppressLint("MissingPermission")
private fun enableLocation(context: Context, map: MapLibreMap, style: Style) {
    try {
        val locationComponent = map.locationComponent
        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(context, style).build()
        )
        locationComponent.isLocationComponentEnabled = true
        locationComponent.renderMode = RenderMode.COMPASS
        locationComponent.cameraMode = CameraMode.NONE
    } catch (e: Exception) {
        Log.e("PlannerMap", "Error enabling location: ${e.message}")
    }
}

@Composable
fun DotWaveAnimation(
    point: Offset,
    onAnimationEnd: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = LinearEasing)
        )
        onAnimationEnd()
    }

    val alpha = 1f - progress.value
    val scale = 0.2f + (progress.value * 2.0f)
    
    Box(
        modifier = Modifier
            .offset { IntOffset(point.x.toInt(), point.y.toInt()) }
            .size(60.dp)
            .graphicsLayer {
                this.translationX = -30.dp.toPx()
                this.translationY = -30.dp.toPx()
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
            }
            .background(DeepGraphite.copy(alpha = 0.5f), CircleShape)
            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
    )
}
