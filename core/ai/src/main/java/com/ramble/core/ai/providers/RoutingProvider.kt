package com.ramble.core.ai.providers

import com.ramble.core.ai.models.POI
import com.ramble.core.ai.models.TransportMode
import com.ramble.core.ai.models.TravelTimeMatrix

interface RoutingProvider {
    suspend fun getTravelTimes(locations: List<POI>, mode: TransportMode): TravelTimeMatrix
}
