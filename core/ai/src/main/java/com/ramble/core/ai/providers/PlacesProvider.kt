package com.ramble.core.ai.providers

import com.ramble.core.ai.models.BoundingBox
import com.ramble.core.ai.models.Coordinates
import com.ramble.core.ai.models.POI

interface PlacesProvider {
    suspend fun getPOIs(coordinates: Coordinates, radiusMeters: Int = 5000): List<POI>
    suspend fun getAccommodation(coordinates: Coordinates, radiusMeters: Int = 5000): List<POI>
}
