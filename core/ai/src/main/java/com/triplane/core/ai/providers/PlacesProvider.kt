package com.triplane.core.ai.providers

import com.triplane.core.ai.models.BoundingBox
import com.triplane.core.ai.models.Coordinates
import com.triplane.core.ai.models.POI

interface PlacesProvider {
    suspend fun getPOIs(coordinates: Coordinates, radiusMeters: Int = 5000): List<POI>
    suspend fun getAccommodation(coordinates: Coordinates, radiusMeters: Int = 5000): List<POI>
}
