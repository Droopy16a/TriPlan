package com.ramble.core.ai.providers.osm

import com.ramble.core.ai.models.Coordinates
import com.ramble.core.ai.providers.GeocodingProvider
import com.ramble.core.location.Properties
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
private data class NominatimResponse(
    val lat: String,
    val lon: String
)

@Serializable
private data class NominatimAddress(
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val hamlet: String? = null,
    val state: String? = null,
    val country: String? = null
)

@Serializable
private data class NominatimReverseResponse(
    val display_name: String,
    val address: NominatimAddress? = null
)

class NominatimGeocodingProvider(private val httpClient: HttpClient) : GeocodingProvider {
    private val cache = mutableMapOf<String, Coordinates>()
    private val reverseCache = mutableMapOf<String, String>()
    private val detailCache = mutableMapOf<String, Properties>()

    override suspend fun geocode(locationName: String): Coordinates? {
        val normalized = locationName.trim().lowercase()
        if (cache.containsKey(normalized)) {
            return cache[normalized]
        }
        
        return try {
            val response: List<NominatimResponse> = httpClient.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", locationName)
                parameter("format", "json")
                parameter("limit", 1)
                header("User-Agent", "Ramble/1.0 (developer@ramble.example)")
            }.body()

            val first = response.firstOrNull()
            if (first != null) {
                val coords = Coordinates(first.lat.toDouble(), first.lon.toDouble())
                cache[normalized] = coords
                coords
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun reverseGeocode(lat: Double, lon: Double): String? {
        val cacheKey = "$lat,$lon"
        if (reverseCache.containsKey(cacheKey)) {
            return reverseCache[cacheKey]
        }

        return try {
            val response: NominatimReverseResponse = httpClient.get("https://nominatim.openstreetmap.org/reverse") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("format", "jsonv2")
                header("User-Agent", "Ramble/1.0 (developer@ramble.example)")
            }.body()
            
            val address = response.display_name
            reverseCache[cacheKey] = address
            address
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Specialized reverse geocode that returns structured properties.
     */
    suspend fun reverseGeocodeToProperties(lat: Double, lon: Double): Properties? {
        val cacheKey = "$lat,$lon"
        if (detailCache.containsKey(cacheKey)) {
            return detailCache[cacheKey]
        }

        return try {
            val response: NominatimReverseResponse = httpClient.get("https://nominatim.openstreetmap.org/reverse") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("format", "jsonv2")
                header("User-Agent", "Ramble/1.0 (developer@ramble.example)")
            }.body()

            val addr = response.address
            if (addr != null) {
                val cityName = addr.city ?: addr.town ?: addr.village ?: addr.hamlet
                val props = Properties(
                    name = cityName,
                    city = cityName,
                    state = addr.state,
                    country = addr.country,
                    lat = lat,
                    lon = lon
                )
                detailCache[cacheKey] = props
                props
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
