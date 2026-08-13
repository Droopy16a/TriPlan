package com.triplane.core.ai.providers

import com.triplane.core.ai.models.POI
import com.triplane.core.ai.models.TransportMode
import com.triplane.core.ai.models.TravelTimeMatrix

interface RoutingProvider {
    suspend fun getTravelTimes(locations: List<POI>, mode: TransportMode): TravelTimeMatrix
}
