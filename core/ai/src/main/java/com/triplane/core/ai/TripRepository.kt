package com.triplane.core.ai

import kotlinx.serialization.Serializable

/**
 * A saved trip that can be displayed as a HeroTripCard on the home screen.
 * Stores both card-level metadata and the full AI itinerary for the workspace.
 */
@Serializable
data class SavedTrip(
    val id: String,
    val title: String,
    val destination: String,
    val dates: String,
    val travelers: String,
    val budget: String,
    val emoji: String = "✈️",
    val itinerary: TripItinerary? = null
)

/**
 * In-memory trip repository. In a real app this would persist to Room/DataStore.
 * Exposed as a singleton so both HomeViewModel and the trip screen can access it.
 */
object TripRepository {
    private val _trips = mutableListOf<SavedTrip>()
    val trips: List<SavedTrip> get() = _trips.toList()

    fun save(trip: SavedTrip) {
        // Replace if same destination already exists, otherwise add
        val idx = _trips.indexOfFirst { it.id == trip.id }
        if (idx >= 0) _trips[idx] = trip else _trips.add(0, trip)
    }

    fun getById(id: String): SavedTrip? = _trips.find { it.id == id }

    fun clear() = _trips.clear()
}
