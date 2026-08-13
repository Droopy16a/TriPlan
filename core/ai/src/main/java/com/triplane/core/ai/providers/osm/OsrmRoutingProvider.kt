package com.triplane.core.ai.providers.osm

import com.triplane.core.ai.models.POI
import com.triplane.core.ai.models.TransportMode
import com.triplane.core.ai.models.TravelTimeMatrix
import com.triplane.core.ai.providers.RoutingProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
private data class OsrmTableResponse(
    val code: String,
    val durations: List<List<Double?>> = emptyList()
)

class OsrmRoutingProvider(private val httpClient: HttpClient) : RoutingProvider {
    
    override suspend fun getTravelTimes(locations: List<POI>, mode: TransportMode): TravelTimeMatrix {
        if (locations.isEmpty()) return TravelTimeMatrix(mode, emptyList(), locations)
        
        // Ensure we don't send too many coordinates to the public API
        // OSRM table service has a limit, typically 100 coordinates, but we'll cap at 30 to be safe
        val safeLocations = locations.take(30)
        
        val profile = when (mode) {
            TransportMode.WALKING -> "foot"
            TransportMode.DRIVING -> "driving"
        }
        
        val coordsStr = safeLocations.joinToString(";") { "${it.coordinates.lon},${it.coordinates.lat}" }
        val url = "https://router.project-osrm.org/table/v1/$profile/$coordsStr"
        
        return try {
            val response: OsrmTableResponse = httpClient.get(url).body()
            
            if (response.code == "Ok") {
                // Convert seconds to minutes
                val minutesDurations = response.durations.map { row ->
                    row.map { seconds ->
                        if (seconds != null) {
                            (seconds / 60.0).roundToInt().toDouble()
                        } else null
                    }
                }
                TravelTimeMatrix(mode, minutesDurations, safeLocations)
            } else {
                TravelTimeMatrix(mode, emptyList(), safeLocations)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            TravelTimeMatrix(mode, emptyList(), safeLocations)
        }
    }
}
