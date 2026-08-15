package com.triplane.feature.trip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.absoluteValue
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.location.modes.CameraMode
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import androidx.core.content.res.ResourcesCompat
import android.content.Context
import android.content.Intent
import com.triplane.core.ai.CommunityTripRepository
import android.net.Uri
import androidx.compose.animation.core.spring
import androidx.core.net.toUri
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.triplane.core.ai.Expense
import com.triplane.core.designsystem.theme.DeepGraphite
import com.triplane.core.designsystem.theme.UIBackgroundGray
import com.triplane.core.designsystem.theme.BrandLightGreen
import com.triplane.core.designsystem.util.clickWithDelay
import com.triplane.core.designsystem.util.parseCurrency
import com.triplane.core.ai.SavedTrip
import com.triplane.core.ai.TripItinerary
import com.triplane.core.ai.TripRepository
import com.triplane.core.ai.TripStep
import com.triplane.core.ai.AiPlannerService
import com.triplane.core.ai.providers.GeocodingProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripWorkspaceScreen(
    tripId: String? = null,
    transitionComplete: Boolean = true,
    onBackClick: () -> Unit
) {
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    var maplibreMapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    
    // Set padding to 75% of screen height to force the "center" into the top quarter
    val bottomPaddingPx = remember(density, config) { 
        with(density) { (config.screenHeightDp * 0.75f).dp.roundToPx() } 
    }
    val boundsPaddingPx = remember(density) { with(density) { 40.dp.roundToPx() } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            maplibreMapRef?.let { map ->
                map.style?.let { style ->
                    enableLocation(context, map, style)
                }
            }
        }
    }

    val allTrips by TripRepository.trips.collectAsState()
    val currentTrip = remember(tripId, allTrips) { 
        allTrips.find { it.id == tripId } ?: CommunityTripRepository.searchTrips("").find { it.id == tripId }
    }
    val tripName = currentTrip?.let { trip ->
        if (trip.title.isNotBlank()) trip.title
        else {
            val city = trip.destination.substringBefore(",").trim()
            val year = trip.itinerary?.days?.firstOrNull()?.date?.substringBefore("-") ?: "2026"
            "$city $year"
        }
    } ?: "Kyoto 2026"
    val aiItinerary = currentTrip?.itinerary
    val geocodingProvider = remember(context) { AiPlannerService(context).geocodingProvider }

    val dayCount = aiItinerary?.days?.size ?: 1
    val pagerState = rememberPagerState(pageCount = { dayCount })

    var isMapVisible by remember { mutableStateOf(false) }

    // Update map only after the page has fully settled — never mid-swipe.
    // snapshotFlow emits null while scrolling is in progress; filterNotNull + distinctUntilChanged
    // guarantee the expensive map work runs exactly once per page, after the finger lifts.
    LaunchedEffect(maplibreMapRef, aiItinerary, isMapVisible) {
        val map = maplibreMapRef ?: return@LaunchedEffect
        if (!isMapVisible) return@LaunchedEffect

        snapshotFlow { if (pagerState.isScrollInProgress) null else pagerState.currentPage }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { currentPage ->
                val style = map.style ?: return@collect

                val currentDaySteps = if (aiItinerary != null) {
                    aiItinerary.days.getOrNull(currentPage)?.steps
                        ?.filter { it.lat != null && it.lon != null }
                        ?: emptyList()
                } else {
                    // Fallback for Day 1
                    if (currentPage == 0) {
                        listOf(
                            LatLng(49.0097, 2.5479) to "Paris CDG",
                            LatLng(34.4320, 135.2304) to "Osaka KIX",
                            LatLng(35.0116, 135.7681) to "Kyoto"
                        ).map { (pos, title) ->
                            TripStep(
                                time = "", title = title, description = "", category = "",
                                lat = pos.latitude, lon = pos.longitude
                            )
                        }
                    } else emptyList()
                }

                // Update Route
                val routeSource = style.getSourceAs<GeoJsonSource>("routes-source")
                if (routeSource != null) {
                    val features = if (currentDaySteps.size > 1) {
                        val points = currentDaySteps.map { Point.fromLngLat(it.lon!!, it.lat!!) }
                        listOf(Feature.fromGeometry(LineString.fromLngLats(points)))
                    } else emptyList()
                    routeSource.setGeoJson(FeatureCollection.fromFeatures(features))
                }

                // Update Markers
                val markerSource = style.getSourceAs<GeoJsonSource>("markers-source")
                if (markerSource != null) {
                    val features = currentDaySteps.mapIndexed { index, step ->
                        val f = Feature.fromGeometry(Point.fromLngLat(step.lon!!, step.lat!!))
                        val iconId = if (aiItinerary != null) {
                            "marker-$currentPage-$index"
                        } else {
                            "marker-fallback-$index"
                        }
                        f.addStringProperty("icon-id", iconId)
                        f
                    }
                    markerSource.setGeoJson(FeatureCollection.fromFeatures(features))
                }

                // Zoom to bounds
                if (currentDaySteps.isNotEmpty()) {
                    try {
                        if (currentDaySteps.size > 1) {
                            val bounds = LatLngBounds.Builder().apply {
                                currentDaySteps.forEach { include(LatLng(it.lat!!, it.lon!!)) }
                            }.build()
                            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, boundsPaddingPx), 1000)
                        } else {
                            // Single step — just zoom to it directly, no bounds needed
                            val step = currentDaySteps[0]
                            map.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(LatLng(step.lat!!, step.lon!!), 16.0),
                                1000
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("TripMap", "Camera update failed: ${e.message}")
                    }
                }
            }
    }

    // Sheet and UI only appear after the transition animation completes
    var sheetReady by remember { mutableStateOf(false) }
    val sheetPeekHeight by animateDpAsState(
        targetValue = if (sheetReady) 350.dp else 0.dp,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "sheetPeekHeight"
    )
    LaunchedEffect(transitionComplete) {
        if (transitionComplete) sheetReady = true
    }

    BackHandler(enabled = transitionComplete) {
        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
        } else {
            onBackClick()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = sheetPeekHeight,
        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetContainerColor = Color.White,
        containerColor = Color.White,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            TripSheetContent(
                tripId = tripId,
                pagerState = pagerState,
                geocodingProvider = geocodingProvider,
                onTripDeleted = onBackClick,
                onLocationSelected = { point ->
                    scope.launch {
                        scaffoldState.bottomSheetState.partialExpand()

                        val map = maplibreMapRef

                        if (map != null) {
                            val currentCenter = map.cameraPosition.target
                            val distance = currentCenter?.distanceTo(point) ?: 0.0

                            if (distance > 100_000 && currentCenter != null) {
                                val bounds = LatLngBounds.Builder()
                                    .include(currentCenter)
                                    .include(point)
                                    .build()
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160), 800)
                                kotlinx.coroutines.delay(800)

                                map.animateCamera(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.Builder()
                                            .target(point)
                                            .zoom(16.0)
                                            .bearing(0.0)
                                            .tilt(0.0)
                                            .build()
                                    ),
                                    700
                                )
                            } else {
                                map.animateCamera(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.Builder()
                                            .target(point)
                                            .zoom(16.0)
                                            .bearing(0.0)
                                            .tilt(0.0)
                                            .build()
                                    ),
                                    1000
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    MapLibre.getInstance(ctx)
                    val options = MapLibreMapOptions.createFromAttributes(ctx, null)
                        .textureMode(true)

                    MapView(ctx, options).apply {
                        getMapAsync { map ->
                            maplibreMapRef = map
                            map.setPadding(0, 0, 0, bottomPaddingPx)
                            map.uiSettings.isCompassEnabled = false
                            map.uiSettings.isLogoEnabled = false
                            map.uiSettings.isAttributionEnabled = false
                            
                            map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                                // Load typeface once — ResourcesCompat.getFont() hits asset I/O
                                // and must not be called per-bitmap inside the loop.
                                val typeface = ResourcesCompat.getFont(
                                    ctx, com.triplane.core.designsystem.R.font.poppins_semibold
                                )

                                // Add ALL potential images to style
                                aiItinerary?.days?.forEachIndexed { dayIdx, day ->
                                    day.steps.forEachIndexed { stepIdx, step ->
                                        val bitmap = createCustomMarkerBitmap(ctx, step.title, typeface)
                                        style.addImage("marker-$dayIdx-$stepIdx", bitmap)
                                    }
                                }

                                // Fallback images
                                listOf("Paris CDG", "Osaka KIX", "Kyoto").forEachIndexed { i, label ->
                                    style.addImage("marker-fallback-$i", createCustomMarkerBitmap(ctx, label, typeface))
                                }

                                // Sources
                                style.addSource(GeoJsonSource("routes-source"))
                                style.addSource(GeoJsonSource("markers-source"))

                                // Layers
                                style.addLayer(LineLayer("routes-layer", "routes-source").withProperties(
                                    PropertyFactory.lineColor("#2B2D42"),
                                    PropertyFactory.lineWidth(2f),
                                    PropertyFactory.lineDasharray(arrayOf(2f, 2f)),
                                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                                ))

                                style.addLayer(SymbolLayer("markers-layer", "markers-source").withProperties(
                                    PropertyFactory.iconImage(Expression.get("icon-id")),
                                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                                    PropertyFactory.iconOffset(arrayOf(0f, 4f)),
                                    PropertyFactory.iconAllowOverlap(true)
                                ))

                                isMapVisible = true

                                // Enable user location
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
                        }
                    }
                },
                update = { view -> },
                onRelease = { view -> view.onDestroy() },
                modifier = Modifier.fillMaxSize()
            )

            if (!isMapVisible) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White))
            }

            AnimatedVisibility(
                visible = transitionComplete,
                enter = fadeIn(animationSpec = tween(300))
            ) {
                FilledIconButton(
                    onClick = { clickWithDelay(scope, onClick = onBackClick) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = DeepGraphite
                    ),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 16.dp, start = 16.dp)
                        .size(48.dp)
                        .shadow(15.dp, RoundedCornerShape(50), spotColor = Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.TopStart)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        }
    }
}

@Composable
fun TripSheetContent(
    tripId: String?,
    pagerState: PagerState,
    geocodingProvider: GeocodingProvider,
    onTripDeleted: () -> Unit = {},
    onLocationSelected: (LatLng) -> Unit
) {
    val allTrips by TripRepository.trips.collectAsState()
    val currentTrip = remember(tripId, allTrips) { allTrips.find { it.id == tripId } }
    val tripName = currentTrip?.let { trip ->
        if (trip.title.isNotBlank()) trip.title
        else {
            val city = trip.destination.substringBefore(",").trim()
            val year = trip.itinerary?.days?.firstOrNull()?.date?.substringBefore("-") ?: "2026"
            "$city $year"
        }
    } ?: "Kyoto 2026"
    val aiItinerary = currentTrip?.itinerary
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Itinerary", "Expenses", "Members")

    var prefilledTitle by remember { mutableStateOf<String?>(null) }
    var prefilledAmount by remember { mutableStateOf<Double?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val totalBudget = remember(currentTrip?.budget) {
        parseCurrency(currentTrip?.budget).let { if (it == 0.0) 1500.0 else it }
    }
    val spentBudget = remember(aiItinerary, currentTrip?.expenses) {
        val itineraryCost = aiItinerary?.days?.flatMap { it.steps }?.sumOf { it.estimatedCost ?: 0.0 } ?: 0.0
        val manualExpenses = currentTrip?.expenses?.sumOf { it.amount } ?: 0.0
        itineraryCost + manualExpenses
    }

    val stopFlingCollapseConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return available
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(Color.White)
            .navigationBarsPadding()
            .nestedScroll(stopFlingCollapseConnection)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(text = tripName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DeepGraphite)
                Spacer(modifier = Modifier.width(6.dp))
                Text(currentTrip?.emoji ?: "⛩️", style = MaterialTheme.typography.headlineSmall)
            }
            var showMenu by remember { mutableStateOf(false) }
            Box {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, DeepGraphite.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        modifier = Modifier.size(20.dp),
                        tint = DeepGraphite
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Trip", style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            showMenu = false
                            showEditDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = DeepGraphite
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Trip", style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            showMenu = false
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out my trip to ${currentTrip?.destination}! ${currentTrip?.title} (${currentTrip?.dates})")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = DeepGraphite
                            )
                        }
                    )
//                    DropdownMenuItem(
//                        text = { Text("Duplicate Trip", style = MaterialTheme.typography.bodyMedium) },
//                        onClick = {
//                            showMenu = false
//                            // Add duplicate action here
//                        },
//                        leadingIcon = {
//                            Icon(
//                                Icons.Default.ContentCopy,
//                                contentDescription = null,
//                                modifier = Modifier.size(18.dp),
//                                tint = DeepGraphite
//                            )
//                        }
//                    )
                    HorizontalDivider(color = UIBackgroundGray)
                    DropdownMenuItem(
                        text = { Text("Delete Trip", style = MaterialTheme.typography.bodyMedium, color = Color.Red) },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.Red
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(currentTrip?.dates ?: "May 10 – May 16, 2025", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (currentTrip != null) "${currentTrip.travelers} members" else "5 members", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = DeepGraphite,
            divider = { HorizontalDivider(color = UIBackgroundGray) },
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTabIndex])
                            .padding(horizontal = 32.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(BrandLightGreen)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Medium else FontWeight.Normal,
                            color = DeepGraphite,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTabIndex) {
            0 -> ItineraryView(
                itinerary = aiItinerary,
                pagerState = pagerState,
                geocodingProvider = geocodingProvider,
                onLocationSelected = onLocationSelected,
                onAddToExpenses = { title, amount ->
                    prefilledTitle = title
                    prefilledAmount = amount
                    selectedTabIndex = 1
                }
            )
            1 -> com.triplane.feature.expenses.ExpenseScreen(
                totalBudget = totalBudget,
                spentBudget = spentBudget,
                expenses = currentTrip?.expenses ?: emptyList(),
                memberNames = currentTrip?.memberNames?.takeIf { it.isNotEmpty() } 
                    ?: TripRepository.getDefaultMembers(currentTrip?.travelers),
                prefilledTitle = prefilledTitle,
                prefilledAmount = prefilledAmount,
                onPrefillHandled = {
                    prefilledTitle = null
                    prefilledAmount = null
                },
                onAddExpense = { expense ->
                    tripId?.let { TripRepository.addExpense(it, expense) }
                },
                onUpdateExpense = { updated ->
                    tripId?.let { TripRepository.updateExpense(it, updated) }
                },
                onDeleteExpense = { expenseId ->
                    tripId?.let { TripRepository.deleteExpense(it, expenseId) }
                }
            )
            else -> {
                MembersView(
                    members = currentTrip?.memberNames?.takeIf { it.isNotEmpty() }
                        ?: TripRepository.getDefaultMembers(currentTrip?.travelers),
                    expenses = currentTrip?.expenses ?: emptyList()
                )
            }
        }
    }

    if (showEditDialog && currentTrip != null) {
        EditTripDialog(
            trip = currentTrip,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedTrip ->
                TripRepository.save(updatedTrip)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && tripId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Trip") },
            text = { Text("Are you sure you want to delete this trip? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        TripRepository.deleteTrip(tripId)
                        showDeleteDialog = false
                        onTripDeleted()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color.DarkGray)) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditTripDialog(
    trip: SavedTrip,
    onDismiss: () -> Unit,
    onConfirm: (SavedTrip) -> Unit
) {
    var title by remember { mutableStateOf(trip.title) }
    var emoji by remember { mutableStateOf(trip.emoji) }
    var travelers by remember { mutableStateOf(trip.travelers) }
    var budget by remember { mutableStateOf(trip.budget) }
    var dates by remember { mutableStateOf(trip.dates) }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    val formColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = DeepGraphite,
        focusedLabelColor    = DeepGraphite,
        cursorColor          = DeepGraphite,
        unfocusedBorderColor = Color(0xFFDDDDDD),
        unfocusedLabelColor  = Color(0xFF888888)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Edit Trip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepGraphite
                    )
                    Text(
                        "Update your trip details",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepGraphite.copy(alpha = 0.5f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = DeepGraphite.copy(alpha = 0.6f))
                }
            }
        },
        text = {
            val emojis = listOf("✈️", "⛩️", "🗼", "🎡", "🏖️", "🏔️", "🏜️", "🛳️", "🚗", "🚆", "🏨", "🗺️")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Cover Emoji", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(emojis.size) { index ->
                            val e = emojis[index]
                            val isSelected = emoji == e
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) DeepGraphite else UIBackgroundGray)
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) DeepGraphite else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(e, fontSize = 22.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Trip Title") },
                    placeholder = { Text("e.g. My Adventure") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = formColors
                )

                OutlinedTextField(
                    value = dates,
                    onValueChange = { },
                    label = { Text("Dates") },
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
                        disabledLeadingIconColor = DeepGraphite.copy(alpha = 0.6f)
                    ),
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = travelers,
                        onValueChange = { travelers = it },
                        label = { Text("Travelers") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = formColors,
                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) }
                    )
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it },
                        label = { Text("Budget") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = formColors,
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val oldTravelersCount = trip.travelers.filter { it.isDigit() }.toDoubleOrNull() ?: 1.0
                    val newTravelersCount = travelers.filter { it.isDigit() }.toDoubleOrNull() ?: 1.0

                    val updatedMemberNames = if (travelers != trip.travelers) {
                        TripRepository.getDefaultMembers(travelers)
                    } else {
                        trip.memberNames
                    }

                    val itinerary = trip.itinerary
                    val updatedItinerary = if (travelers != trip.travelers && itinerary != null && oldTravelersCount > 0) {
                        val ratio = newTravelersCount / oldTravelersCount
                        itinerary.copy(
                            days = itinerary.days.map { day ->
                                day.copy(
                                    steps = day.steps.map { step ->
                                        step.copy(estimatedCost = step.estimatedCost?.let { it * ratio })
                                    }
                                )
                            },
                            estimatedTotalCost = itinerary.estimatedTotalCost?.let { it * ratio }
                        )
                    } else {
                        itinerary
                    }

                    onConfirm(trip.copy(
                        title = title,
                        emoji = emoji,
                        travelers = travelers,
                        budget = budget,
                        dates = dates,
                        memberNames = updatedMemberNames,
                        itinerary = updatedItinerary
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepGraphite)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        val startDate = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                        val endDate = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalDate()
                        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
                        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                        dates = "${startDate.format(formatter)} – ${endDate.format(formatter)} ($days days)"
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f),
                showModeToggle = false
            )
        }
    }
}

@Composable
fun ItineraryView(
    itinerary: TripItinerary? = null,
    pagerState: PagerState,
    geocodingProvider: GeocodingProvider,
    onLocationSelected: (LatLng) -> Unit,
    onAddToExpenses: (String, Double) -> Unit = { _, _ -> }
) {
    val dayCount = itinerary?.days?.size ?: 1

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            pageSpacing = 0.dp
        ) { pageIndex ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (itinerary != null) {
                    val day = itinerary.days[pageIndex]
                    item {
                        // Read pager state inside graphicsLayer so offsets only trigger
                        // a cheap draw-phase update instead of a full recomposition.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
                                    this.alpha = (1f - pageOffset * 1.5f).coerceIn(0f, 1f)
                                    this.translationX = pageOffset * 200f
                                }
                                .background(UIBackgroundGray.copy(alpha = 0.5f))
                                .padding(horizontal = 32.dp, vertical = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Day ${day.dayNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DeepGraphite)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    val dateText = try {
                                        val date = LocalDate.parse(day.date)
                                        date.format(DateTimeFormatter.ofPattern("MMM d, E", Locale.getDefault()))
                                    } catch (_: Exception) {
                                        day.date
                                    }
                                    Text(text = dateText, style = MaterialTheme.typography.bodyMedium, color = DeepGraphite.copy(alpha = 0.6f))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    day.temperature?.let { temp ->
                                        val (weatherIcon, iconColor) = when (day.weatherCondition?.lowercase()) {
                                            "cloudy" -> Icons.Default.Cloud to Color(0xFF90A4AE) // Blue Grey
                                            "rainy", "stormy" -> Icons.Default.WaterDrop to Color(0xFF4FC3F7) // Light Blue
                                            else -> Icons.Default.WbSunny to Color(0xFFFDB813) // Yellow
                                        }
                                        Icon(imageVector = weatherIcon, contentDescription = day.weatherCondition, tint = iconColor, modifier = Modifier.size(18.dp))
                                        Text(text = temp, style = MaterialTheme.typography.bodyMedium, color = DeepGraphite.copy(alpha = 0.8f))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = day.theme, style = MaterialTheme.typography.bodyMedium, color = DeepGraphite.copy(alpha = 0.8f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    day.steps.forEachIndexed { index, step ->
                        item {
                            val icon = when (step.category) {
                                "Flight" -> Icons.Default.FlightTakeoff
                                "Transport" -> Icons.Default.DirectionsTransit
                                "Food" -> Icons.Default.Restaurant
                                else -> Icons.Default.Place
                            }
                            Box(modifier = Modifier.padding(horizontal = 32.dp)) {
                                TimelineItem(
                                    icon = icon, text = step.title, time = step.time,
                                    description = step.description, price = step.estimatedCost,
                                    lat = step.lat, lon = step.lon,
                                    geocodingProvider = geocodingProvider,
                                    onClick = {
                                        val lat = step.lat
                                        val lon = step.lon
                                        if (lat != null && lon != null) onLocationSelected(LatLng(lat, lon))
                                    },
                                    onAddToExpenses = {
                                        step.estimatedCost?.let { onAddToExpenses(step.title, it) }
                                    }
                                )
                            }
                        }
                        if (index < day.steps.size - 1) {
                            item { Box(modifier = Modifier.padding(horizontal = 32.dp)) { TimelineDots(count = 3) } }
                        }
                    }

                    // See Itinerary Button at the end of the day
                    val validSteps = day.steps.filter { it.lat != null && it.lon != null }
                    if (validSteps.size >= 2) {
                        item {
                            val context = LocalContext.current
                            Spacer(Modifier.height(24.dp))
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
                                    .padding(horizontal = 32.dp)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepGraphite
                                )
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("See Itinerary in Maps", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    if (pageIndex == 0) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().graphicsLayer {
                                    val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
                                    this.alpha = (1f - pageOffset * 1.5f).coerceIn(0f, 1f)
                                    this.translationX = pageOffset * 200f
                                }
                                    .background(UIBackgroundGray.copy(alpha = 0.5f))
                                    .padding(horizontal = 32.dp, vertical = 20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Day 1", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DeepGraphite)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = "May 10, Sat", style = MaterialTheme.typography.bodyMedium, color = DeepGraphite.copy(alpha = 0.6f))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(imageVector = Icons.Default.WbSunny, contentDescription = "Sunny", tint = Color(0xFFFDB813), modifier = Modifier.size(18.dp))
                                        Text(text = "22°C", style = MaterialTheme.typography.bodyMedium, color = DeepGraphite.copy(alpha = 0.8f))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item { Box(modifier = Modifier.padding(horizontal = 32.dp)) { TimelineItem(icon = Icons.Default.FlightTakeoff, text = "Paris CDG (Terminal 2E)", time = "13:30", lat = 49.0097, lon = 2.5479, geocodingProvider = geocodingProvider, onClick = { onLocationSelected(LatLng(49.0097, 2.5479)) }) } }
                        item { Box(modifier = Modifier.padding(horizontal = 32.dp)) { TimelineDots(count = 6) } }
                        item { Box(modifier = Modifier.padding(horizontal = 32.dp)) { TimelineItem(icon = Icons.Default.FlightLand, text = "Osaka KIX (Terminal 1)", time = "08:30 (+1)", lat = 34.4320, lon = 135.2304, geocodingProvider = geocodingProvider, onClick = { onLocationSelected(LatLng(34.4320, 135.2304)) }) } }
                        item { Box(modifier = Modifier.padding(horizontal = 32.dp)) { TimelineDots(count = 3) } }
                        item { Box(modifier = Modifier.padding(horizontal = 32.dp)) { TimelineItem(icon = Icons.Default.DirectionsTransit, text = "Haruka Express to Kyoto", time = "10:15", lat = 35.0116, lon = 135.7681, geocodingProvider = geocodingProvider, onClick = { onLocationSelected(LatLng(35.0116, 135.7681)) }) } }
                    }
                }
            }
        }

        if (dayCount > 1) {
            Row(Modifier.padding(bottom = 24.dp).fillMaxWidth().align(Alignment.BottomCenter), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                repeat(dayCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) BrandLightGreen else UIBackgroundGray
                    Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).size(8.dp))
                }
            }
        }
    }
}

@Composable
fun TimelineItem(
    icon: ImageVector,
    text: String,
    time: String,
    description: String? = null,
    price: Double? = null,
    lat: Double? = null,
    lon: Double? = null,
    geocodingProvider: GeocodingProvider? = null,
    onClick: () -> Unit = {},
    onAddToExpenses: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth().clickable { clickWithDelay(scope, onClick = onClick) }.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp).padding(top = 4.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = text, style = MaterialTheme.typography.titleSmall, color = DeepGraphite)
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (price != null) {
                Spacer(modifier = Modifier.height(2.dp))
                val priceFormatted = String.format(Locale.US, "$ %.0f", price)
                Text(text = priceFormatted, style = MaterialTheme.typography.labelSmall, color = BrandLightGreen)
            }

            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (price != null) {
                    TextButton(
                        onClick = onAddToExpenses,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "Expenses",
                            fontSize = 12.sp,
                            color = DeepGraphite,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = DeepGraphite
                        )
                    }
                }

                if (lat != null && lon != null) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                // reverseGeocode is a network call — dispatch to IO so it
                                // never blocks the main thread while the user waits to open Maps.
                                val address = withContext(Dispatchers.IO) {
                                    geocodingProvider?.reverseGeocode(lat, lon)
                                }
                                val searchQuery = if (address != null) "$text $address" else text
                                val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                                val uri = "https://www.google.com/maps/search/?api=1&query=$encodedQuery"
                                val fallbackUri = "geo:$lat,$lon?q=${encodedQuery}"
                                val intent = try {
                                    Intent(Intent.ACTION_VIEW, uri.toUri())
                                } catch (e: Exception) {
                                    Intent(Intent.ACTION_VIEW, fallbackUri.toUri())
                                }
                                context.startActivity(intent)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "See more",
                            fontSize = 12.sp,
                            color = DeepGraphite,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = DeepGraphite
                        )
                    }
                }
            }
        }
        Text(text = time, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}

@Composable
fun TimelineDots(count: Int) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .width(28.dp)
            .height((count * 12).dp)
            .padding(vertical = 4.dp)
    ) {
        drawLine(
            color = UIBackgroundGray,
            start = Offset(x = size.width / 2, y = 0f),
            end = Offset(x = size.width / 2, y = size.height),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    }
}

// cachedTypeface: pass a pre-loaded Typeface to avoid repeated ResourcesCompat.getFont()
// asset I/O when creating many markers in a loop. Falls back to loading from resources
// when called standalone (e.g. tests, single-use callsites).
fun createCustomMarkerBitmap(
    context: Context,
    text: String,
    cachedTypeface: android.graphics.Typeface? = null
): Bitmap {
    val typeface = cachedTypeface
        ?: ResourcesCompat.getFont(context, com.triplane.core.designsystem.R.font.poppins_semibold)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#2B2D42"); textSize = 45f; this.typeface = typeface; textAlign = Paint.Align.CENTER }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#2B2D42") }
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    val dotRadius = 10f; val haloRadius = 16f; val gap = 8f
    val textBounds = AndroidRect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)
    val width = (textBounds.width() + 80).coerceAtLeast((haloRadius * 2 + 8).toInt())
    val height = textBounds.height() + gap.toInt() + (haloRadius * 2).toInt() + 4
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = width / 2f; val cy = height - haloRadius
    canvas.drawCircle(cx, cy, haloRadius, haloPaint)
    canvas.drawCircle(cx, cy, dotRadius, dotPaint)
    val textY = cy - haloRadius - gap - textBounds.bottom
    canvas.drawText(text, cx, textY, textPaint)
    return bitmap
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
        Log.e("TripMap", "Error enabling location: ${e.message}")
    }
}
