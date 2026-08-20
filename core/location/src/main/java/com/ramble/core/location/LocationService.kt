package com.ramble.core.location

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class LocationService {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            })
        }
    }

    private val suggestionCache = android.util.LruCache<String, List<Properties>>(50)

    suspend fun getAutocompleteSuggestions(query: String): List<Properties> {
        val normalizedKey = query.trim().lowercase()
        if (normalizedKey.isBlank()) return emptyList()

        val cached = suggestionCache.get(normalizedKey)
        if (cached != null) {
            return cached
        }
        
        return try {
            val response: PhotonResponse = client.get("https://photon.komoot.io/api/") {
                parameter("q", query)
                parameter("limit", 5)
            }.body()
            
            val results = response.features.map { feature ->
                feature.properties.copy(
                    lat = feature.geometry.coordinates.getOrNull(1),
                    lon = feature.geometry.coordinates.getOrNull(0)
                )
            }
            if (results.isNotEmpty()) {
                suggestionCache.put(normalizedKey, results)
            }
            results
        } catch (e: Exception) {
            android.util.Log.e("LocationService", "Error fetching suggestions: ${e.message}", e)
            emptyList()
        }
    }
}
