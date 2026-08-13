package com.triplane.core.ai.providers

import com.triplane.core.ai.models.Coordinates

interface GeocodingProvider {
    suspend fun geocode(locationName: String): Coordinates?
    suspend fun reverseGeocode(lat: Double, lon: Double): String?
}
