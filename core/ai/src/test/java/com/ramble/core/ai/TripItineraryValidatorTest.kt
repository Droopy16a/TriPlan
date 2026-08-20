package com.ramble.core.ai

import com.ramble.core.ai.models.AccommodationType
import com.ramble.core.ai.models.Coordinates
import com.ramble.core.ai.models.POI
import com.ramble.core.ai.models.POIType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripItineraryValidatorTest {

    @Test
    fun testIsPureLodging() {
        val pureHotel = POI(
            id = "hotel_1",
            name = "Hotel Villa Cora",
            category = "Accommodation",
            coordinates = Coordinates(43.76, 11.25),
            type = POIType.ACCOMMODATION,
            accommodationType = AccommodationType.HOTEL,
            osmTags = mapOf("tourism" to "hotel")
        )
        assertTrue(TripItineraryValidator.isPureLodging(pureHotel))

        val hotelRestaurant = POI(
            id = "hotel_rest_1",
            name = "Hotel Villa Cora Restaurant",
            category = "Restaurant",
            coordinates = Coordinates(43.76, 11.25),
            type = POIType.GENERAL,
            osmTags = mapOf("tourism" to "hotel", "amenity" to "restaurant")
        )
        assertFalse(TripItineraryValidator.isPureLodging(hotelRestaurant))

        val museum = POI(
            id = "museum_1",
            name = "Uffizi Gallery",
            category = "Museum",
            coordinates = Coordinates(43.76, 11.25),
            type = POIType.GENERAL,
            osmTags = mapOf("tourism" to "museum")
        )
        assertFalse(TripItineraryValidator.isPureLodging(museum))
    }

    @Test
    fun testFlorenceBrokenItineraryRegressionFixture() {
        // Broken Florence itinerary where lodging POIs are recommended as Activity and Nightlife steps
        val hotelVillaCora = POI(
            id = "osm_node_101",
            name = "Hotel Villa Cora",
            category = "Accommodation",
            coordinates = Coordinates(43.765, 11.248),
            type = POIType.ACCOMMODATION,
            accommodationType = AccommodationType.HOTEL,
            osmTags = mapOf("tourism" to "hotel")
        )

        val helvetiaBristol = POI(
            id = "osm_node_102",
            name = "Helvetia & Bristol",
            category = "Accommodation",
            coordinates = Coordinates(43.771, 11.252),
            type = POIType.ACCOMMODATION,
            accommodationType = AccommodationType.HOTEL,
            osmTags = mapOf("tourism" to "hotel")
        )

        val uffizi = POI(
            id = "osm_node_103",
            name = "Uffizi Gallery",
            category = "Museum",
            coordinates = Coordinates(43.768, 11.255),
            type = POIType.GENERAL,
            osmTags = mapOf("tourism" to "museum")
        )

        val candidatePois = listOf(hotelVillaCora, helvetiaBristol, uffizi)

        val brokenChunk = TripItinerary(
            destination = "Florence",
            title = "Florence Highlights",
            summary = "Exploring Florence",
            budgetAllocation = BudgetAllocation("30%", "30%", "20%", "20%"),
            days = listOf(
                TripDay(
                    dayNumber = 1,
                    date = "2026-09-01",
                    theme = "Culture & Relaxation",
                    steps = listOf(
                        TripStep(time = "09:00 AM", title = "Hotel Villa Cora", description = "Hotel Check-in", category = "Accommodation", lat = 43.765, lon = 11.248),
                        TripStep(time = "10:30 AM", title = "Uffizi Gallery", description = "Art Museum", category = "Activity", lat = 43.768, lon = 11.255),
                        TripStep(time = "03:00 PM", title = "Hotel Villa Cora", description = "Explore the hotel gardens", category = "Activity", lat = 43.765, lon = 11.248)
                    )
                ),
                TripDay(
                    dayNumber = 2,
                    date = "2026-09-02",
                    theme = "Nightlife",
                    steps = listOf(
                        TripStep(time = "10:00 AM", title = "Uffizi Gallery", description = "Visit museum", category = "Activity", lat = 43.768, lon = 11.255),
                        TripStep(time = "09:00 PM", title = "Helvetia & Bristol", description = "Nightlife experience", category = "Nightlife", lat = 43.771, lon = 11.252)
                    )
                )
            )
        )

        val range = 1..2
        val dates = listOf(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-02"))

        // Validate without candidates (legacy mode) - would miss pure lodging used as Activity
        val legacyValidation = TripItineraryValidator.validateChunk(brokenChunk, range, dates, emptyList())
        assertTrue("Legacy validator allowed hotel as activity step", legacyValidation.isValid)

        // Validate with candidate POIs (new semantic validation)
        val newValidation = TripItineraryValidator.validateChunk(brokenChunk, range, dates, candidatePois)
        assertFalse("New validator should fail broken lodging-as-activity itinerary", newValidation.isValid)
        assertTrue(
            newValidation.errors.any { it.contains("Day 1 Activity step 'Hotel Villa Cora' resolves to a lodging-only POI") }
        )
        assertTrue(
            newValidation.errors.any { it.contains("Day 2 Nightlife step 'Helvetia & Bristol' resolves to a lodging-only POI") }
        )
    }

    @Test
    fun testValidItineraryWithHotelRestaurantPassesValidation() {
        val hotelRestaurant = POI(
            id = "osm_node_201",
            name = "Grand Hotel Roof Garden",
            category = "Restaurant",
            coordinates = Coordinates(43.770, 11.250),
            type = POIType.GENERAL,
            osmTags = mapOf("tourism" to "hotel", "amenity" to "restaurant")
        )

        val uffizi = POI(
            id = "osm_node_202",
            name = "Uffizi Gallery",
            category = "Museum",
            coordinates = Coordinates(43.768, 11.255),
            type = POIType.GENERAL,
            osmTags = mapOf("tourism" to "museum")
        )

        val candidatePois = listOf(hotelRestaurant, uffizi)

        val validChunk = TripItinerary(
            destination = "Florence",
            title = "Florence Art & Food",
            summary = "Exploring Florence",
            budgetAllocation = BudgetAllocation("30%", "30%", "20%", "20%"),
            days = listOf(
                TripDay(
                    dayNumber = 1,
                    date = "2026-09-01",
                    theme = "Art & Fine Dining",
                    steps = listOf(
                        TripStep(time = "10:00 AM", title = "Uffizi Gallery", description = "Museum tour", category = "Activity", lat = 43.768, lon = 11.255),
                        TripStep(time = "01:00 PM", title = "Grand Hotel Roof Garden", description = "Lunch at hotel restaurant", category = "Food", lat = 43.770, lon = 11.250)
                    )
                )
            )
        )

        val range = 1..1
        val dates = listOf(LocalDate.parse("2026-09-01"))

        val validation = TripItineraryValidator.validateChunk(validChunk, range, dates, candidatePois)
        assertTrue("Valid itinerary with hotel restaurant should pass validation", validation.isValid)
    }
}
