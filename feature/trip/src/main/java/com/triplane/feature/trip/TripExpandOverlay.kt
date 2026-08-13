package com.triplane.feature.trip

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.triplane.core.ai.TripRepository
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import androidx.core.content.res.ResourcesCompat
import androidx.compose.ui.util.lerp

/**
 * A full-screen animated overlay that expands from [startBounds] (card pixel bounds)
 * to fill the entire screen, giving a cinematic map-expand effect.
 * Calls [onComplete] when the animation finishes.
 */
@Composable
fun TripExpandOverlay(
    tripId: String? = null,
    startBounds: Rect,
    screenWidthPx: Float,
    screenHeightPx: Float,
    onComplete: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val currentTrip = remember(tripId) { tripId?.let { TripRepository.getById(it) } }

    LaunchedEffect(Unit) {
        // Start animation immediately for maximum responsiveness.
        // GPU transforms (graphicsLayer) ensure this stays smooth even with vector maps.
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        onComplete()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress.value
                    
                    // GPU-accelerated scaling and translation. 
                    // No layout passes are triggered on the MapView during animation.
                    this.scaleX = lerp(startBounds.width / screenWidthPx, 1f, p)
                    this.scaleY = lerp(startBounds.height / screenHeightPx, 1f, p)
                    this.translationX = lerp(startBounds.left, 0f, p)
                    this.translationY = lerp(startBounds.top, 0f, p)
                    this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    
                    val cornerRadiusDp = lerp(32f, 0f, p)
                    this.shape = RoundedCornerShape(cornerRadiusDp.dp)
                    this.clip = true
                }
        ) {
            AndroidView(
                factory = { context ->
                    MapLibre.getInstance(context)
                    val options = MapLibreMapOptions.createFromAttributes(context, null)
                        .textureMode(true)
                    
                    MapView(context, options).apply {
                        getMapAsync { map ->
                            map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                                val validSteps = currentTrip?.itinerary?.days?.flatMap { it.steps }
                                    ?.filter { it.lat != null && it.lon != null }
                                    ?: emptyList()

                                val routePoints = if (validSteps.isNotEmpty()) {
                                    validSteps.map { LatLng(it.lat!!, it.lon!!) to it.title }
                                } else {
                                    // Fallback if no trip data (e.g. static card)
                                    listOf(
                                        LatLng(49.0097, 2.5479) to "Paris CDG",
                                        LatLng(34.4320, 135.2304) to "Osaka KIX",
                                        LatLng(35.0116, 135.7681) to "Kyoto"
                                    )
                                }

                                // Camera set to match the target workspace exactly
                                if (routePoints.isNotEmpty()) {
                                    val bounds = LatLngBounds.Builder().apply {
                                        routePoints.forEach { include(it.first) }
                                    }.build()
                                    try {
                                        if (routePoints.size > 1) {
                                            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                                        } else {
                                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(routePoints.first().first, 10.0))
                                        }
                                    } catch (e: Exception) {
                                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(routePoints.first().first, 10.0))
                                    }
                                }

                                // Add Routes
                                if (routePoints.size > 1) {
                                    val points = routePoints.map { Point.fromLngLat(it.first.longitude, it.first.latitude) }
                                    style.addSource(GeoJsonSource("routes-source", Feature.fromGeometry(LineString.fromLngLats(points))))
                                    
                                    style.addLayer(LineLayer("routes-layer", "routes-source").withProperties(
                                        PropertyFactory.lineColor("#2B2D42"),
                                        PropertyFactory.lineWidth(2f),
                                        PropertyFactory.lineDasharray(arrayOf(2f, 2f)),
                                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                                    ))
                                }

                                // Markers
                                routePoints.forEachIndexed { index, (pos, label) ->
                                    val markerBitmap = createExpandMarkerBitmap(context, label)
                                    style.addImage("marker-$index", markerBitmap)
                                    
                                    val sourceId = "marker-source-$index"
                                    style.addSource(GeoJsonSource(sourceId, Feature.fromGeometry(
                                        Point.fromLngLat(pos.longitude, pos.latitude)
                                    )))
                                    
                                    style.addLayer(SymbolLayer("marker-layer-$index", sourceId).withProperties(
                                        PropertyFactory.iconImage("marker-$index"),
                                        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                                        PropertyFactory.iconAllowOverlap(true)
                                    ))
                                }
                            }
                        }
                    }
                },
                update = { view -> },
                onRelease = { view -> view.onDestroy() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun createExpandMarkerBitmap(context: android.content.Context, text: String): Bitmap {
    val typeface = ResourcesCompat.getFont(context, com.triplane.core.designsystem.R.font.poppins_semibold)

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

    val bounds = AndroidRect()
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
