package com.triplane.core.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import com.triplane.core.ai.providers.osm.NominatimGeocodingProvider
import com.triplane.core.ai.providers.osm.OsmPlacesProvider
import com.triplane.core.ai.providers.osm.OsrmRoutingProvider
import com.triplane.core.ai.providers.weather.OpenMeteoWeatherProvider
import com.triplane.core.ai.pipeline.CandidateFilterEngine
import com.triplane.core.ai.models.TransportMode
import com.triplane.core.ai.models.POI
import com.triplane.core.ai.models.Coordinates
import kotlinx.serialization.encodeToString

class AiPlannerService(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val cloudModel = GenerativeModel(
        modelName = "gemini-3.1-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    private val nanoModel = GenerativeModel(
        modelName = "gemini-nano",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                isLenient = true
            })
        }
    }

    val geocodingProvider = NominatimGeocodingProvider(httpClient)
    val placesProvider = OsmPlacesProvider(httpClient)
    val routingProvider = OsrmRoutingProvider(httpClient)
    val weatherProvider = OpenMeteoWeatherProvider(httpClient)
    val filterEngine = CandidateFilterEngine()

    /**
     * Entry point for generating a trip. 
     * Now primarily delegates to TripPlanningOrchestrator for chunking logic.
     */
    suspend fun generateTrip(
        departure: String,
        destination: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        travelers: String,
        budget: String,
        preferences: String
    ): Result<TripItinerary> {
        val orchestrator = TripPlanningOrchestrator(this)
        return orchestrator.generateItinerary(
            departure, destination, startDate, endDate, travelers, budget, preferences
        )
    }

    /**
     * Generates a single chunk of the itinerary.
     */
    suspend fun generateTripChunk(
        departure: String,
        destination: String,
        travelers: String,
        budget: String,
        preferences: String,
        tripStartDate: LocalDate,
        tripEndDate: LocalDate,
        totalDuration: Int,
        chunkStartDate: LocalDate,
        chunkEndDate: LocalDate,
        chunkStartDay: Int,
        chunkEndDay: Int,
        chunkNumber: Int,
        totalChunks: Int,
        candidates: List<POI>,
        accommodationCandidates: List<POI>,
        weatherForecast: String,
        alreadyUsedPois: List<String>,
        remainingBudget: String
    ): Result<TripItinerary> {
        return try {
            val isOnline = isNetworkAvailable()
            
            val poisJson = json.encodeToString(candidates)
            val accommodationJson = json.encodeToString(accommodationCandidates)
            
            val contextData = """
                CANDIDATE PLACES (DO NOT INVENT PLACES NOT IN THIS LIST):
                $poisJson
                
                ACCOMMODATION CANDIDATES (ONLY RECOMMEND FROM THIS LIST):
                $accommodationJson
                
                WEATHER FORECAST:
                $weatherForecast

                ALREADY USED PLACES (Do not repeat these unless necessary):
                ${alreadyUsedPois.joinToString(", ")}
            """.trimIndent()

            val prompt = buildChunkPrompt(
                departure, destination, travelers, budget, preferences,
                tripStartDate, tripEndDate, totalDuration,
                chunkStartDate, chunkEndDate, chunkStartDay, chunkEndDay,
                chunkNumber, totalChunks, contextData, remainingBudget
            )

            val response = if (!isOnline) {
                nanoModel.generateContent(prompt)
            } else {
                val hasAiCore = hasAiCore()
                // For chunks, we prefer cloud to ensure quality
                if (!hasAiCore || totalDuration > 5) {
                    cloudModel.generateContent(prompt)
                } else {
                    nanoModel.generateContent(prompt)
                }
            }

            val text = response.text
            if (text != null) {
                val itinerary = json.decodeFromString<TripItinerary>(text)
                Result.success(itinerary)
            } else {
                Result.failure(Exception("AI returned empty response for chunk $chunkNumber"))
            }
        } catch (e: Exception) {
            Log.e("AiPlanner", "Error generating trip chunk", e)
            Result.failure(e)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun hasAiCore(): Boolean {
        return false
    }

    private fun buildChunkPrompt(
        departure: String,
        destination: String,
        travelers: String,
        totalBudget: String,
        preferences: String,
        tripStartDate: LocalDate,
        tripEndDate: LocalDate,
        totalDuration: Int,
        chunkStartDate: LocalDate,
        chunkEndDate: LocalDate,
        chunkStartDay: Int,
        chunkEndDay: Int,
        chunkNumber: Int,
        totalChunks: Int,
        contextData: String,
        remainingBudget: String
    ): String {
        return """
            You are an expert travel planner. You are generating a SPECIFIC PORTION of a complete trip itinerary.
            
            COMPLETE TRIP CONTEXT:
            Departure location: $departure
            Destination: $destination
            Start date: $tripStartDate
            End date: $tripEndDate
            Total duration: $totalDuration days
            Travelers: $travelers
            Total budget: $totalBudget
            Preferences: $preferences
            
            CURRENT GENERATION CHUNK:
            Chunk: $chunkNumber of $totalChunks
            Chunk start date: $chunkStartDate
            Chunk end date: $chunkEndDate
            Chunk day range: Day $chunkStartDay to Day $chunkEndDay
            Remaining budget to work with for this and future chunks: $remainingBudget
            
            $contextData
            
            STRICT REQUIREMENTS:
            1. Generate EXACTLY ${chunkEndDay - chunkStartDay + 1} days for this chunk.
            2. The 'dayNumber' must range from $chunkStartDay to $chunkEndDay sequentially.
            3. The 'date' for each day must be exactly correct (YYYY-MM-DD).
            4. A normal full day should contain approximately 4–6 meaningful steps.
            5. Steps can include: Food, Activity, Transport, FreeTime, Accommodation.
            6. Arrival/departure days may contain fewer steps.
            7. Do not repeat places listed in 'ALREADY USED PLACES'.
            8. Only use places from 'CANDIDATE PLACES' for activities/food.
            9. Only recommend accommodation from the supplied 'ACCOMMODATION CANDIDATES'.
            10. Do not invent hotels, prices, or availability. If accommodation price is not in the data, set estimatedCost to null. If no accommodation is available, state that in the summary.
            11. Take the departure location ($departure) into account for the first and last day (e.g., travel times, airports, train stations).
            12. Ensure the schedule is realistic with travel times.
            
            Return the result ONLY as a valid JSON object matching this schema exactly:
            {
              "destination": "$destination",
              "title": "String (catchy title for the chunk or trip)",
              "summary": "String (brief overview of this chunk)",
              "budgetAllocation": {
                "accommodation": "String (e.g. 30%)",
                "food": "String",
                "transport": "String",
                "activities": "String"
              },
              "days": [
                {
                  "dayNumber": Int,
                  "date": "YYYY-MM-DD",
                  "theme": "String",
                  "temperature": "String (e.g. '22°C' derived from WEATHER FORECAST, or null if unknown)",
                  "weatherCondition": "String (choose one: 'Sunny', 'Cloudy', 'Rainy', 'Snowy', 'Stormy', or null)",
                  "steps": [
                    {
                      "time": "String (e.g. 09:00 AM)",
                      "title": "String (Real place from Candidates)",
                      "description": "String",
                      "category": "String (Food, Activity, Transport, FreeTime, or Accommodation)",
                      "estimatedCost": "Double (e.g. 25.50)",
                      "lat": "Double",
                      "lon": "Double"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }
}
