package com.ramble.core.ai

import android.util.Log
import com.ramble.core.ai.models.Coordinates
import com.ramble.core.ai.models.POI
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TripPlanningOrchestrator(private val aiService: AiPlannerService? = null) {

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

        val service = aiService ?: return Result.failure(Exception("AiPlannerService not initialized"))
        val coords = service.geocodingProvider.geocode(destination)
            ?: return Result.failure(Exception("Could not geocode destination: $destination"))

        val allPois = service.placesProvider.getPOIs(coords)
        val filteredPois = service.filterEngine.filterAndRank(allPois, limit = 50, adhocPreferences = preferences)
        
        val allAccommodation = service.placesProvider.getAccommodation(coords)
        val filteredAccommodation = service.filterEngine.filterAndRankAccommodation(allAccommodation, limit = 15, adhocPreferences = preferences)
        
        val weather = service.weatherProvider.getForecast(coords.lat, coords.lon, startDate, endDate)

        val chunks = defineChunks(totalDuration, startDate)
        val chunkPlanner = TripChunkPlanner(service)
        
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
            summary = buildDeterministicSummary(first.destination, allDays),
            budgetAllocation = computeBudgetAllocation(allDays),
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

    internal fun buildDeterministicSummary(destination: String, days: List<TripDay>): String {
        val totalDays = days.size
        val dayWord = if (totalDays == 1) "day" else "days"
        val topThemes = days
            .map { it.theme }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)

        return if (topThemes.isNotEmpty()) {
            "A $totalDays-$dayWord journey exploring $destination, featuring ${topThemes.joinToString(", ")}."
        } else {
            "A $totalDays-$dayWord journey exploring $destination."
        }
    }

    internal fun computeBudgetAllocation(days: List<TripDay>): BudgetAllocation {
        var accommodationCost = 0.0
        var foodCost = 0.0
        var transportCost = 0.0
        var activitiesCost = 0.0

        for (day in days) {
            for (step in day.steps) {
                val cost = step.estimatedCost ?: 0.0
                val cat = step.category.trim().lowercase()
                when {
                    cat == "accommodation" -> accommodationCost += cost
                    cat == "food" || cat == "cafe" -> foodCost += cost
                    cat == "transport" -> transportCost += cost
                    else -> activitiesCost += cost
                }
            }
        }

        val totalCost = accommodationCost + foodCost + transportCost + activitiesCost
        if (totalCost <= 0.0) {
            return BudgetAllocation(
                accommodation = "25%",
                food = "25%",
                transport = "25%",
                activities = "25%"
            )
        }

        val rawAcc = (accommodationCost / totalCost) * 100
        val rawFood = (foodCost / totalCost) * 100
        val rawTrans = (transportCost / totalCost) * 100
        val rawAct = (activitiesCost / totalCost) * 100

        var accPct = kotlin.math.round(rawAcc).toInt()
        var foodPct = kotlin.math.round(rawFood).toInt()
        var transPct = kotlin.math.round(rawTrans).toInt()
        var actPct = kotlin.math.round(rawAct).toInt()

        val sum = accPct + foodPct + transPct + actPct
        val diff = 100 - sum
        if (diff != 0) {
            val categories = mutableListOf(
                Pair("acc", rawAcc to accPct),
                Pair("food", rawFood to foodPct),
                Pair("trans", rawTrans to transPct),
                Pair("act", rawAct to actPct)
            )
            val maxIdx = categories.indices.maxByOrNull { categories[it].second.first } ?: 3
            when (maxIdx) {
                0 -> accPct += diff
                1 -> foodPct += diff
                2 -> transPct += diff
                3 -> actPct += diff
            }
        }

        return BudgetAllocation(
            accommodation = "$accPct%",
            food = "$foodPct%",
            transport = "$transPct%",
            activities = "$actPct%"
        )
    }

    data class ChunkDefinition(
        val startDay: Int,
        val endDay: Int,
        val startDate: LocalDate,
        val endDate: LocalDate
    )
}
