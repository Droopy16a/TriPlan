package com.ramble.core.ai.pipeline

import com.ramble.core.ai.models.POI
import kotlin.math.abs

class CandidateFilterEngine {

    fun filterAndRank(rawPois: List<POI>, limit: Int = 20, adhocPreferences: String? = null): List<POI> {
        val profile = try { com.ramble.core.ai.ProfileRepository.profile.value } catch (_: Throwable) { com.ramble.core.ai.UserProfile() }
        val interests = (profile.interests + (adhocPreferences?.let { parseInterests(it) } ?: emptyList())).map { it.lowercase() }.distinct()
        val foodPrefs = (profile.foodPreferences + (adhocPreferences?.let { parseFoodPrefs(it) } ?: emptyList())).map { it.lowercase() }.distinct()
        
        // 0. Exclude pure lodging accommodations from general POI candidates
        val nonAccommodationPois = rawPois.filter { poi ->
            !com.ramble.core.ai.TripItineraryValidator.isPureLodging(poi) &&
            !isAccommodationName(poi.name)
        }

        // 1. Deduplicate (by name and close proximity)
        val deduplicated = deduplicate(nonAccommodationPois)

        // 2. Rank
        val ranked = deduplicated.map { poi ->
            var score = 1.0
            
            // Boost for having metadata
            if (!poi.website.isNullOrBlank()) score += 2.0
            if (!poi.openingHours.isNullOrBlank()) score += 1.5
            if (!poi.phone.isNullOrBlank()) score += 1.0
            if (!poi.cuisine.isNullOrBlank()) score += 1.0
            if (!poi.priceLevel.isNullOrBlank()) score += 0.5
            
            // Boost based on user profile interests matched against structured OSM category tags
            val categoryTags = listOfNotNull(
                poi.category.lowercase(),
                poi.osmTags["tourism"]?.lowercase(),
                poi.osmTags["amenity"]?.lowercase(),
                poi.osmTags["leisure"]?.lowercase(),
                poi.osmTags["shop"]?.lowercase(),
                poi.osmTags["historic"]?.lowercase()
            )

            val matchesInterest = interests.any { interest ->
                val synonyms = when(interest) {
                    "nightlife" -> listOf("bar", "pub", "nightclub", "club", "casino", "lounge", "cabaret", "dance")
                    "sports" -> listOf("stadium", "sports", "pitch", "golf", "arena", "fitness", "gym", "sports_centre")
                    "nature" -> listOf("park", "reserve", "beach", "forest", "water", "trail", "garden", "mountain", "hiking", "nature_reserve", "viewpoint")
                    "culture" -> listOf("museum", "gallery", "historic", "attraction", "monument", "artwork", "castle", "temple", "church", "cathedral", "theatre", "archaeological_site")
                    "shopping" -> listOf("shop", "mall", "boutique", "market", "supermarket", "clothes", "jewelry")
                    "food" -> listOf("restaurant", "cafe", "bakery", "fast_food", "deli", "bistro", "brasserie")
                    else -> listOf(interest)
                }
                
                categoryTags.any { tag ->
                    tag == interest || synonyms.any { syn -> tag.contains(syn) }
                }
            }
            
            if (matchesInterest) {
                score += 5.0
            }

            // Direct check in adhocPreferences for any specific nightlife or other keywords matched against category tags
            if (adhocPreferences != null) {
                val adhocLower = adhocPreferences.lowercase()
                if (adhocLower.contains("nightlife") || adhocLower.contains("night club") || adhocLower.contains("party")) {
                    if (categoryTags.any { tag -> tag.contains("bar") || tag.contains("pub") || tag.contains("nightclub") }) {
                        score += 3.0
                    }
                }
            }
            
            // Boost based on user profile food preferences
            val cuisineLower = poi.cuisine?.lowercase() ?: ""
            if (foodPrefs.any { pref -> cuisineLower.contains(pref) || categoryTags.any { tag -> tag.contains(pref) } }) {
                score += 4.0
            }
            
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
        
        return result.take(limit).sortedByDescending { it.relevanceScore }
    }

    private fun isAccommodationName(name: String): Boolean {
        val lower = name.lowercase()
        val regex = Regex("""\b(hotel|hôtel|aparthotel|hostel|resort|motel|guesthouse|guest house|b&b|bed & breakfast|bed and breakfast|staycity|novotel|mercure|ibis|hilton|marriott|hyatt|radisson|sheraton|westin|crowne plaza|holiday inn|albergo|pensione|locanda|hostal|posada|riad|suites?)\b""", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(lower)
    }

    private fun parseInterests(prefs: String): List<String> {
        // Simple parsing of "Interests: Nightlife, Culture"
        val interestLine = prefs.lines().find { it.contains("Interests:", ignoreCase = true) } 
            ?: prefs.split(";").find { it.contains("Interests:", ignoreCase = true) }
        
        return interestLine?.substringAfter("Interests:")?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
    }

    private fun parseFoodPrefs(prefs: String): List<String> {
        val foodLine = prefs.lines().find { it.contains("Food:", ignoreCase = true) }
            ?: prefs.split(";").find { it.contains("Food:", ignoreCase = true) }
            
        return foodLine?.substringAfter("Food:")?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
    }

    fun filterAndRankAccommodation(rawAccommodation: List<POI>, limit: Int = 10, adhocPreferences: String? = null): List<POI> {
        val profile = try { com.ramble.core.ai.ProfileRepository.profile.value } catch (_: Throwable) { com.ramble.core.ai.UserProfile() }
        val accPrefs = (profile.accommodationPreference + (adhocPreferences?.let { parseAccPrefs(it) } ?: emptyList())).map { it.lowercase() }.distinct()
        
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
            
            // Boost based on accommodation preferences
            val typeLower = poi.accommodationType?.name?.lowercase() ?: ""
            val nameLower = poi.name.lowercase()
            
            if (accPrefs.any { typeLower.contains(it) || nameLower.contains(it) }) {
                score += 6.0
            }
            
            poi.copy(relevanceScore = score)
        }.sortedByDescending { it.relevanceScore }

        return ranked.take(limit)
    }

    private fun parseAccPrefs(prefs: String): List<String> {
        val accLine = prefs.lines().find { it.contains("Accommodation:", ignoreCase = true) }
            ?: prefs.split(";").find { it.contains("Accommodation:", ignoreCase = true) }
            
        return accLine?.substringAfter("Accommodation:")?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
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
