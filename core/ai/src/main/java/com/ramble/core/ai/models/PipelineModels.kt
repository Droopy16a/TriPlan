package com.ramble.core.ai.models

import kotlinx.serialization.Serializable

@Serializable
data class Coordinates(val lat: Double, val lon: Double)

@Serializable
data class BoundingBox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double
)

enum class POIType {
    GENERAL,
    ACCOMMODATION
}

enum class AccommodationType {
    HOTEL,
    HOSTEL,
    GUEST_HOUSE,
    BED_AND_BREAKFAST,
    APARTMENT,
    MOTEL,
    RESORT,
    CHALET,
    LODGE,
    CAMPSITE,
    CARAVAN_SITE,
    GLAMPING,
    FARM,
    ALPINE_HUT,
    WILDERNESS_HUT,
    OTHER
}

@Serializable
data class POI(
    val id: String,
    val name: String,
    val category: String, // e.g. "Restaurant", "Museum", "Accommodation"
    val coordinates: Coordinates,
    val type: POIType = POIType.GENERAL,
    val accommodationType: AccommodationType? = null,
    val address: String? = null,
    val openingHours: String? = null,
    val website: String? = null,
    val phone: String? = null,
    val cuisine: String? = null,
    val priceLevel: String? = null,
    val stars: Int? = null,
    val rooms: Int? = null,
    val beds: Int? = null,
    val amenities: List<String> = emptyList(),
    val relevanceScore: Double = 0.0 // Computed later by ranking engine
)

enum class TransportMode {
    WALKING, DRIVING
}

@Serializable
data class TravelTimeMatrix(
    val mode: TransportMode,
    // durations[i][j] is the travel time in minutes from POI i to POI j
    val durations: List<List<Double?>>,
    val locations: List<POI>
)

@Serializable
data class WeatherInfo(
    val destination: String,
    val startDate: String,
    val endDate: String,
    val forecast: String // summary of the forecast
)
