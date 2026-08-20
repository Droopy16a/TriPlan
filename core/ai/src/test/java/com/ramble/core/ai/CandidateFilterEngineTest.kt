package com.ramble.core.ai

import com.ramble.core.ai.models.Coordinates
import com.ramble.core.ai.models.POI
import com.ramble.core.ai.models.POIType
import com.ramble.core.ai.pipeline.CandidateFilterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateFilterEngineTest {

    @Test
    fun testPureLodgingExcludedFromGeneralPoiCandidates() {
        val filterEngine = CandidateFilterEngine()

        val pureHotel = POI(
            id = "hotel_1",
            name = "Culture Hotel Centro Storico",
            category = "Accommodation",
            coordinates = Coordinates(43.76, 11.25),
            type = POIType.ACCOMMODATION,
            website = "https://culturehotel.com",
            phone = "+391234567",
            osmTags = mapOf("tourism" to "hotel")
        )

        val realMuseum = POI(
            id = "museum_1",
            name = "Uffizi Gallery",
            category = "Museum",
            coordinates = Coordinates(43.768, 11.255),
            type = POIType.GENERAL,
            osmTags = mapOf("tourism" to "museum")
        )

        val rawPois = listOf(pureHotel, realMuseum)
        val filtered = filterEngine.filterAndRank(rawPois, limit = 10, adhocPreferences = "Interests: Culture")

        assertFalse("Pure lodging POI should be excluded from general candidate pool", filtered.any { it.id == pureHotel.id })
        assertTrue("Real museum should be included", filtered.any { it.id == realMuseum.id })
    }

    @Test
    fun testInterestMatchingUsesOsmCategoryTagsNotPoiNames() {
        val filterEngine = CandidateFilterEngine()

        // Hotel restaurant has amenity=restaurant co-tag
        val hotelRestaurant = POI(
            id = "rest_1",
            name = "Hotel Villa Cora Restaurant",
            category = "Restaurant",
            coordinates = Coordinates(43.76, 11.25),
            type = POIType.GENERAL,
            osmTags = mapOf("tourism" to "hotel", "amenity" to "restaurant")
        )

        val cultureCenterHotel = POI(
            id = "hotel_2",
            name = "Culture Hotel",
            category = "Accommodation",
            coordinates = Coordinates(43.77, 11.26),
            type = POIType.GENERAL, // general type, but tagged tourism=hotel
            osmTags = mapOf("tourism" to "hotel")
        )

        val rawPois = listOf(hotelRestaurant, cultureCenterHotel)
        val filtered = filterEngine.filterAndRank(rawPois, limit = 10, adhocPreferences = "Interests: Culture")

        assertFalse("Pure lodging POI 'Culture Hotel' should not match Culture interest or be included", filtered.any { it.name == "Culture Hotel" })
    }
}
