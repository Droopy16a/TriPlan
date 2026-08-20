package com.ramble.core.ai

import com.ramble.core.ai.models.Coordinates
import com.ramble.core.ai.models.POI
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TripRepositoryCacheTest {

    @Test
    fun testSavedTripSerializationAndCaching() {
        val trip = SavedTrip(
            id = "test_trip_101",
            title = "Paris Gateway",
            destination = "Paris, France",
            dates = "Sep 1 - Sep 5 (5 days)",
            travelers = "2 travelers",
            budget = "$ 2000 total",
            preferences = "Culture, Food",
            emoji = "🗼",
            itinerary = TripItinerary(
                destination = "Paris, France",
                title = "Paris Gateway",
                summary = "A 5-day journey exploring Paris.",
                budgetAllocation = BudgetAllocation("40%", "30%", "10%", "20%"),
                days = listOf(
                    TripDay(
                        dayNumber = 1,
                        date = "2026-09-01",
                        theme = "Landmarks",
                        steps = listOf(
                            TripStep(time = "09:00 AM", title = "Eiffel Tower", description = "Visit monument", category = "Activity", lat = 48.858, lon = 2.294)
                        )
                    )
                ),
                estimatedTotalCost = 1500.0
            )
        )

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val jsonString = json.encodeToString(SavedTrip.serializer(), trip)
        val decoded = json.decodeFromString(SavedTrip.serializer(), jsonString)

        assertEquals(trip.id, decoded.id)
        assertEquals(trip.title, decoded.title)
        assertEquals(trip.destination, decoded.destination)
        assertEquals(1, decoded.itinerary?.days?.size)
        assertEquals("Eiffel Tower", decoded.itinerary?.days?.first()?.steps?.first()?.title)
    }
}
