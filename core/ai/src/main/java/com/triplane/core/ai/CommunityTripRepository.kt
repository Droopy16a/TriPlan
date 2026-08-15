package com.triplane.core.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CommunityTripRepository {

    private val initialTrips = listOf(
        SavedTrip(
            id = "comm-paris-01",
            title = "Romantic Paris Getaway",
            destination = "Paris, France",
            dates = "Anytime",
            travelers = "2",
            budget = "$$ 3,000",
            emoji = "🗼",
            memberNames = listOf("Alice", "Bob")
        ),
        SavedTrip(
            id = "comm-tokyo-01",
            title = "Tokyo Neon Lights",
            destination = "Tokyo, Japan",
            dates = "Anytime",
            travelers = "1",
            budget = "$ 1,500",
            emoji = "🗼",
            memberNames = listOf("Charlie")
        ),
        SavedTrip(
            id = "comm-ny-01",
            title = "New York Weekend",
            destination = "New York, USA",
            dates = "Anytime",
            travelers = "4",
            budget = "$$$ 5,000",
            emoji = "🗽",
            memberNames = listOf("David", "Eve", "Frank", "Grace")
        ),
        SavedTrip(
            id = "comm-rome-01",
            title = "Ancient Rome Exploration",
            destination = "Rome, Italy",
            dates = "Anytime",
            travelers = "2",
            budget = "$$ 2,500",
            emoji = "🏛️",
            memberNames = listOf("Hannah", "Ian")
        ),
        SavedTrip(
            id = "comm-kyoto-01",
            title = "Kyoto Temples & Gardens",
            destination = "Kyoto, Japan",
            dates = "Anytime",
            travelers = "2",
            budget = "$$ 2,000",
            emoji = "⛩️",
            memberNames = listOf("Jack", "Kelly")
        ),
        SavedTrip(
            id = "comm-bali-01",
            title = "Bali Beach Retreat",
            destination = "Bali, Indonesia",
            dates = "Anytime",
            travelers = "2",
            budget = "$ 1,200",
            emoji = "🏝️",
            memberNames = listOf("Leo", "Mia")
        )
    )

    private val _communityTrips = MutableStateFlow(initialTrips)
    val communityTrips: StateFlow<List<SavedTrip>> = _communityTrips.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            // Fetch images sequentially or in parallel
            val updatedTrips = initialTrips.map { trip ->
                val city = trip.destination.substringBefore(",").trim()
                val imageUrl = PexelsService.getCityImage(city)
                trip.copy(imageUrl = imageUrl)
            }
            _communityTrips.value = updatedTrips
        }
    }

    fun searchTrips(query: String): List<SavedTrip> {
        val lowerQuery = query.trim().lowercase()
        val currentTrips = _communityTrips.value
        if (lowerQuery.isEmpty()) {
            return currentTrips
        }
        return currentTrips.filter { trip ->
            trip.destination.lowercase().contains(lowerQuery) ||
            trip.title.lowercase().contains(lowerQuery)
        }
    }
}
