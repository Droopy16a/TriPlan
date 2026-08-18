package com.ramble.feature.home.component

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramble.core.ai.SavedTrip
import com.ramble.core.designsystem.theme.*
import com.ramble.core.designsystem.util.clickWithDelay
import com.ramble.core.designsystem.util.parseCurrency
import com.ramble.core.designsystem.util.shimmer
import com.ramble.feature.home.OverlappingAvatars
import com.ramble.feature.home.createMiniMapMarkerBitmap
import com.ramble.feature.home.vibrateDevice
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

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
    val density = LocalDensity.current

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
                    textAlign = TextAlign.Center,
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
        val manualExpenses = savedTrip.expenses?.filter { !it.isSettlement }?.sumOf { it.amount } ?: 0.0

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

                key(savedTrip.id) {
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
                                        val itinerary = savedTrip.itinerary
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

                                        // Routes
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

                                        // Markers
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
                
                // Progress Bar
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(UIBorderGray)
                ) {
                    val progressWidth = maxWidth * budgetProgress
                    
                    Box(
                        modifier = Modifier
                            .width(progressWidth)
                            .fillMaxHeight()
                            .background(BrandLightGreen)
                    )
                    
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
fun PopularTripCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .shimmer()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .width(120.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .width(80.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
        )
    }
}

@Composable
fun PopularTripCard(
    trip: SavedTrip? = null,
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
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
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
            overflow = TextOverflow.Ellipsis,
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
