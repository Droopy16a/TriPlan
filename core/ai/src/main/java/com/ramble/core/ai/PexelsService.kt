package com.ramble.core.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class PexelsResponse(
    val photos: List<PexelsPhoto>
)

@Serializable
data class PexelsPhoto(
    val id: Int,
    val src: PexelsPhotoSrc
)

@Serializable
data class PexelsPhotoSrc(
    val portrait: String,
    val large: String,
    val medium: String,
    val original: String? = null
)

object PexelsService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getCityImage(city: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.PEXELS_API_KEY
        if (apiKey.isBlank()) return@withContext null

        try {
            val response = client.get("https://api.pexels.com/v1/search") {
                header("Authorization", apiKey)
                parameter("query", city)
                parameter("per_page", 1)
                parameter("orientation", "portrait")
            }
            
            val pexelsResponse: PexelsResponse = response.body()
            pexelsResponse.photos.firstOrNull()?.src?.portrait ?: pexelsResponse.photos.firstOrNull()?.src?.large
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
