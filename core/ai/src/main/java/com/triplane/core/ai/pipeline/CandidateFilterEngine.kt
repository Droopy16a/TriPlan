package com.triplane.core.ai.pipeline

import com.triplane.core.ai.models.POI
import kotlin.math.abs

class CandidateFilterEngine {

    fun filterAndRank(rawPois: List<POI>, limit: Int = 20): List<POI> {
        // 1. Deduplicate (by name and close proximity)
        val deduplicated = deduplicate(rawPois)

        // 2. Rank
        val ranked = deduplicated.map { poi ->
            var score = 1.0
            
            // Boost for having metadata
            if (!poi.website.isNullOrBlank()) score += 2.0
            if (!poi.openingHours.isNullOrBlank()) score += 1.5
            if (!poi.phone.isNullOrBlank()) score += 1.0
            if (!poi.cuisine.isNullOrBlank()) score += 1.0
            if (!poi.priceLevel.isNullOrBlank()) score += 0.5
            
            poi.copy(relevanceScore = score)
        }.sortedByDescending { it.relevanceScore }

        // 3. Ensure a mix of categories (simple heuristic: take top from each category if possible, then fill rest)
        val categories = ranked.groupBy { it.category }
        val result = mutableListOf<POI>()
        
        // Take top 3 from each category to ensure diversity
        categories.forEach { (_, pois) ->
            result.addAll(pois.take(3))
        }
        
        // Fill the rest with remaining highest ranked
        val remaining = ranked.filter { it !in result }
        result.addAll(remaining.take(maxOf(0, limit - result.size)))
        
        return result.take(limit)
    }

    fun filterAndRankAccommodation(rawAccommodation: List<POI>, limit: Int = 10): List<POI> {
        val deduplicated = deduplicate(rawAccommodation)

        val ranked = deduplicated.map { poi ->
            var score = 1.0
            
            if (!poi.website.isNullOrBlank()) score += 2.0
            if (!poi.phone.isNullOrBlank()) score += 1.0
            if (!poi.address.isNullOrBlank()) score += 1.0
            
            if (poi.stars != null) {
                score += poi.stars * 0.5
            }
            if (poi.amenities.isNotEmpty()) {
                score += poi.amenities.size * 0.2
            }
            
            poi.copy(relevanceScore = score)
        }.sortedByDescending { it.relevanceScore }

        return ranked.take(limit)
    }

    private fun deduplicate(pois: List<POI>): List<POI> {
        val unique = mutableListOf<POI>()
        
        for (poi in pois) {
            val isDuplicate = unique.any { existing ->
                // Check if they have the same ID (e.g. from OSM)
                if (existing.id == poi.id) return@any true

                // Check same name (case insensitive) and very close coordinates
                val nameMatch = existing.name.equals(poi.name, ignoreCase = true)
                // Levenshtein or simple contains could be added for better name matching, but this is okay for now.
                // We don't want to over-deduplicate hotels with similar names (e.g. "Hotel Ibis X" vs "Hotel Ibis Y").

                val coordsMatch = abs(existing.coordinates.lat - poi.coordinates.lat) < 0.001 &&
                                  abs(existing.coordinates.lon - poi.coordinates.lon) < 0.001

                nameMatch && coordsMatch
            }
            if (!isDuplicate) {
                unique.add(poi)
            }
        }
        
        return unique
    }
}
