package com.triplane.core.ai

import android.util.Log
import com.triplane.core.ai.models.POI
import java.time.LocalDate

class TripChunkPlanner(private val aiService: AiPlannerService) {

    suspend fun planChunk(
        departure: String,
        destination: String,
        travelers: String,
        budget: String,
        preferences: String,
        tripStartDate: LocalDate,
        tripEndDate: LocalDate,
        totalDuration: Int,
        chunkDef: TripPlanningOrchestrator.ChunkDefinition,
        chunkNumber: Int,
        totalChunks: Int,
        candidates: List<POI>,
        accommodationCandidates: List<POI>,
        weatherForecast: String,
        alreadyUsedPois: List<String>,
        remainingBudget: String
    ): Result<TripItinerary> {
        var lastResult: Result<TripItinerary>? = null
        val maxRetries = 2

        var previousError: String? = null

        for (attempt in 0..maxRetries) {
            Log.d("ChunkPlanner", "Attempt ${attempt + 1} for chunk $chunkNumber")
            
            val result = aiService.generateTripChunk(
                departure = departure,
                destination = destination,
                travelers = travelers,
                budget = budget,
                preferences = preferences,
                tripStartDate = tripStartDate,
                tripEndDate = tripEndDate,
                totalDuration = totalDuration,
                chunkStartDate = chunkDef.startDate,
                chunkEndDate = chunkDef.endDate,
                chunkStartDay = chunkDef.startDay,
                chunkEndDay = chunkDef.endDay,
                chunkNumber = chunkNumber,
                totalChunks = totalChunks,
                candidates = candidates,
                accommodationCandidates = accommodationCandidates,
                weatherForecast = weatherForecast,
                alreadyUsedPois = alreadyUsedPois,
                remainingBudget = remainingBudget,
                previousError = previousError
            )

            if (result.isSuccess) {
                val itinerary = result.getOrThrow()
                val expectedDates = (0 until (chunkDef.endDay - chunkDef.startDay + 1)).map {
                    chunkDef.startDate.plusDays(it.toLong())
                }
                
                val validation = TripItineraryValidator.validateChunk(
                    itinerary,
                    chunkDef.startDay..chunkDef.endDay,
                    expectedDates
                )

                if (validation.isValid) {
                    return result
                } else {
                    val errorMsg = validation.errors.firstOrNull() ?: "Unknown validation error"
                    Log.w("ChunkPlanner", "Validation failed for chunk $chunkNumber: $errorMsg")
                    previousError = errorMsg
                    lastResult = Result.failure(Exception("Chunk validation failed: $errorMsg"))
                }
            } else {
                lastResult = result
            }
        }

        return lastResult ?: Result.failure(Exception("Failed to generate chunk $chunkNumber after $maxRetries retries"))
    }
}
