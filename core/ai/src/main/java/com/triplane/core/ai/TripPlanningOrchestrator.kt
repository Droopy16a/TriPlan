package com.triplane.core.ai

import android.util.Log
import com.triplane.core.ai.models.Coordinates
import com.triplane.core.ai.models.POI
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TripPlanningOrchestrator(private val aiService: AiPlannerService) {

    suspend fun generateItinerary(
        departure: String,
        destination: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        travelers: String,
        budget: String,
        preferences: String
    ): Result<TripItinerary> {
        if (startDate == null || endDate == null) {
            return Result.failure(Exception("Start and end dates are required for reliable generation"))
        }

        val totalDuration = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        Log.d("Orchestrator", "Planning trip for $destination, duration: $totalDuration days")

        val coords = aiService.geocodingProvider.geocode(destination)
            ?: return Result.failure(Exception("Could not geocode destination: $destination"))

        val allPois = aiService.placesProvider.getPOIs(coords)
        val filteredPois = aiService.filterEngine.filterAndRank(allPois, limit = 50)
        
        val allAccommodation = aiService.placesProvider.getAccommodation(coords)
        val filteredAccommodation = aiService.filterEngine.filterAndRankAccommodation(allAccommodation, limit = 15)
        
        val weather = aiService.weatherProvider.getForecast(coords.lat, coords.lon, startDate, endDate)

        val chunks = defineChunks(totalDuration, startDate)
        val chunkPlanner = TripChunkPlanner(aiService)
        
        val completedChunks = mutableListOf<TripItinerary>()
        val alreadyUsedPoiIds = mutableSetOf<String>()
        var remainingBudget = budget

        for (i in chunks.indices) {
            val chunkDef = chunks[i]
            Log.d("Orchestrator", "Generating chunk ${i + 1}/${chunks.size}: Days ${chunkDef.startDay} to ${chunkDef.endDay}")
            
            // Geographic focus: divide POIs if possible, but for now just pass relevant ones
            // In a more advanced version, we could cluster POIs and assign clusters to chunks
            val chunkPois = filteredPois // Simplified: passing all top POIs for now
            
            val chunkResult = chunkPlanner.planChunk(
                departure = departure,
                destination = destination,
                travelers = travelers,
                budget = budget,
                preferences = preferences,
                tripStartDate = startDate,
                tripEndDate = endDate,
                totalDuration = totalDuration,
                chunkDef = chunkDef,
                chunkNumber = i + 1,
                totalChunks = chunks.size,
                candidates = chunkPois,
                accommodationCandidates = filteredAccommodation,
                weatherForecast = weather.forecast,
                alreadyUsedPois = alreadyUsedPoiIds.toList(),
                remainingBudget = remainingBudget
            )

            if (chunkResult.isSuccess) {
                val chunkItinerary: TripItinerary = chunkResult.getOrNull()!!
                completedChunks.add(chunkItinerary)
                
                // Track used POIs to avoid duplicates in next chunks
                chunkItinerary.days.flatMap { it.steps }.forEach { step ->
                    // Find POI ID if it was from candidates
                    filteredPois.find { it.name == step.title }?.let { alreadyUsedPoiIds.add(it.id) }
                }
            } else {
                return Result.failure(chunkResult.exceptionOrNull() ?: Exception("Failed to generate chunk ${i + 1}"))
            }
        }

        return mergeAndValidate(completedChunks, startDate, endDate)
    }

    private fun defineChunks(totalDuration: Int, startDate: LocalDate): List<ChunkDefinition> {
        val chunkSizes = when {
            totalDuration <= 5 -> listOf(totalDuration)
            totalDuration <= 8 -> listOf(totalDuration / 2, totalDuration - totalDuration / 2)
            totalDuration <= 14 -> {
                val size = totalDuration / 3
                listOf(size, size, totalDuration - 2 * size)
            }
            else -> {
                val numChunks = (totalDuration + 3) / 4
                val baseSize = totalDuration / numChunks
                MutableList(numChunks) { baseSize }.apply {
                    val remainder = totalDuration % numChunks
                    for (i in 0 until remainder) {
                        this[i] += 1
                    }
                }
            }
        }

        val chunks = mutableListOf<ChunkDefinition>()
        var currentDay = 1
        var currentDate = startDate
        
        chunkSizes.forEach { size ->
            val endDay = currentDay + size - 1
            val endDate = currentDate.plusDays((size - 1).toLong())
            chunks.add(ChunkDefinition(currentDay, endDay, currentDate, endDate))
            currentDay = endDay + 1
            currentDate = endDate.plusDays(1)
        }
        
        return chunks
    }

    private fun mergeAndValidate(
        chunks: List<TripItinerary>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<TripItinerary> {
        if (chunks.isEmpty()) return Result.failure(Exception("No chunks generated"))

        val first = chunks.first()
        val allDays = chunks.flatMap { it.days }.sortedBy { it.dayNumber }
        
        val merged = TripItinerary(
            destination = first.destination,
            title = first.title,
            summary = chunks.joinToString("\n\n") { it.summary },
            budgetAllocation = first.budgetAllocation,
            days = allDays,
            estimatedTotalCost = allDays.sumOf { day -> 
                day.steps.sumOf { step -> 
                    step.estimatedCost ?: 0.0 
                } 
            }
        )

        val validation = TripItineraryValidator.validateFinalItinerary(merged, startDate, endDate)
        return if (validation.isValid) {
            Result.success(merged)
        } else {
            Result.failure(Exception("Final itinerary validation failed: ${validation.errors.joinToString("; ")}"))
        }
    }

    data class ChunkDefinition(
        val startDay: Int,
        val endDay: Int,
        val startDate: LocalDate,
        val endDate: LocalDate
    )
}
