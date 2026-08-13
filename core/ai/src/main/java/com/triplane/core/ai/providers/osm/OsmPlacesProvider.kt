package com.triplane.core.ai.providers.osm

import com.triplane.core.ai.models.AccommodationType
import com.triplane.core.ai.models.Coordinates
import com.triplane.core.ai.models.POI
import com.triplane.core.ai.models.POIType
import com.triplane.core.ai.providers.PlacesProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class OverpassResponse(
    val elements: List<OverpassElement>
)

@Serializable
data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

@Serializable
data class OverpassElement(
    val id: Long,
    val type: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String> = emptyMap()
)

class OsmPlacesProvider(private val httpClient: HttpClient) : PlacesProvider {
    
    override suspend fun getPOIs(coordinates: Coordinates, radiusMeters: Int): List<POI> {
        val query = """
            [out:json][timeout:25];
            (
              node["amenity"~"restaurant|cafe|bar|fast_food|pharmacy|hospital"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
              node["tourism"~"museum|attraction|gallery|viewpoint|artwork"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
              node["leisure"~"park|beach_resort|water_park"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
              node["shop"~"supermarket|mall|bakery"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
              node["public_transport"~"station"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
              
              way["amenity"~"restaurant|cafe|bar|fast_food|pharmacy|hospital"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
              way["tourism"~"museum|attraction|gallery|viewpoint|artwork"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
              way["leisure"~"park|beach_resort|water_park"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
              way["shop"~"supermarket|mall|bakery"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
            );
            out center;
        """.trimIndent()

        return try {
            val response: OverpassResponse = httpClient.post("https://overpass-api.de/api/interpreter") {
                setBody(query)
            }.body()

            response.elements.mapNotNull { element ->
                parsePOI(element, POIType.GENERAL)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getAccommodation(coordinates: Coordinates, radiusMeters: Int): List<POI> {
        val accommodationTypes = "hotel|hostel|guest_house|motel|apartment|chalet|camp_site|caravan_site|resort|bed_and_breakfast|alpine_hut|wilderness_hut"
        
        val query = """
            [out:json][timeout:25];
            (
              node["tourism"~"$accommodationTypes"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
              way["tourism"~"$accommodationTypes"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
              relation["tourism"~"$accommodationTypes"](around:$radiusMeters,${coordinates.lat},${coordinates.lon});
            );
            out center;
        """.trimIndent()

        return try {
            val response: OverpassResponse = httpClient.post("https://overpass-api.de/api/interpreter") {
                setBody(query)
            }.body()

            response.elements.mapNotNull { element ->
                parsePOI(element, POIType.ACCOMMODATION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parsePOI(element: OverpassElement, type: POIType): POI? {
        val name = element.tags["name"] ?: return null
        
        val elementLat = element.lat ?: element.center?.lat ?: return null
        val elementLon = element.lon ?: element.center?.lon ?: return null
        
        val elementType = element.type ?: "node"
        val poiId = "osm_${elementType}_${element.id}"

        var categoryName = "Other"
        var accommodationType: AccommodationType? = null
        
        if (type == POIType.ACCOMMODATION) {
            val tourismValue = element.tags["tourism"]
            if (tourismValue != null) {
                categoryName = "Accommodation"
                accommodationType = when (tourismValue) {
                    "hotel" -> AccommodationType.HOTEL
                    "hostel" -> AccommodationType.HOSTEL
                    "guest_house" -> AccommodationType.GUEST_HOUSE
                    "bed_and_breakfast" -> AccommodationType.BED_AND_BREAKFAST
                    "apartment" -> AccommodationType.APARTMENT
                    "motel" -> AccommodationType.MOTEL
                    "resort" -> AccommodationType.RESORT
                    "chalet" -> AccommodationType.CHALET
                    "camp_site" -> AccommodationType.CAMPSITE
                    "caravan_site" -> AccommodationType.CARAVAN_SITE
                    "alpine_hut" -> AccommodationType.ALPINE_HUT
                    "wilderness_hut" -> AccommodationType.WILDERNESS_HUT
                    else -> AccommodationType.OTHER
                }
            }
        } else {
            categoryName = when {
                element.tags.containsKey("amenity") -> element.tags["amenity"]
                element.tags.containsKey("tourism") -> element.tags["tourism"]
                element.tags.containsKey("leisure") -> element.tags["leisure"]
                element.tags.containsKey("shop") -> element.tags["shop"]
                element.tags.containsKey("public_transport") -> "transport"
                else -> "other"
            }?.replace("_", " ")?.capitalize(Locale.ROOT) ?: "Other"
        }

        // Parse optional accommodation metadata
        val stars = element.tags["stars"]?.toIntOrNull()
        val rooms = element.tags["rooms"]?.toIntOrNull()
        val beds = element.tags["beds"]?.toIntOrNull()
        
        val amenities = mutableListOf<String>()
        if (element.tags["internet_access"] != null && element.tags["internet_access"] != "no") amenities.add("Wi-Fi")
        if (element.tags["wheelchair"] == "yes") amenities.add("Wheelchair Accessible")
        if (element.tags["parking"] == "yes") amenities.add("Parking")
        if (element.tags["breakfast"] == "yes" || element.tags["breakfast"] == "included") amenities.add("Breakfast")
        if (element.tags["pets"] == "yes") amenities.add("Pets Allowed")

        val website = element.tags["website"] ?: element.tags["contact:website"]
        val phone = element.tags["phone"] ?: element.tags["contact:phone"]

        return POI(
            id = poiId,
            name = name,
            category = categoryName,
            coordinates = Coordinates(elementLat, elementLon),
            type = type,
            accommodationType = accommodationType,
            address = buildAddress(element.tags),
            openingHours = element.tags["opening_hours"],
            website = website,
            phone = phone,
            cuisine = element.tags["cuisine"],
            priceLevel = null,
            stars = stars,
            rooms = rooms,
            beds = beds,
            amenities = amenities
        )
    }

    private fun buildAddress(tags: Map<String, String>): String? {
        val street = tags["addr:street"]
        val housenumber = tags["addr:housenumber"]
        val city = tags["addr:city"] ?: tags["addr:town"] ?: tags["addr:village"] ?: tags["addr:municipality"]
        val postcode = tags["addr:postcode"]
        
        val addressParts = mutableListOf<String>()
        if (housenumber != null && street != null) {
            addressParts.add("$housenumber $street")
        } else if (street != null) {
            addressParts.add(street)
        }
        
        val cityZip = listOfNotNull(postcode, city).joinToString(" ")
        if (cityZip.isNotBlank()) {
            addressParts.add(cityZip)
        }
        
        return if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            null
        }
    }
}
