package com.ramble.core.ai.providers.osm

import com.ramble.core.ai.models.AccommodationType
import com.ramble.core.ai.models.Coordinates
import com.ramble.core.ai.models.POIType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OsmPlacesProviderTest {

    @Test
    fun `test parsing node, way, and relation responses`() = runBlocking {
        val mockResponseJson = """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 123,
                  "lat": 48.8566,
                  "lon": 2.3522,
                  "tags": {
                    "name": "Node Hotel",
                    "tourism": "hotel",
                    "stars": "4",
                    "website": "https://nodehotel.com",
                    "addr:street": "Rue de Rivoli",
                    "addr:housenumber": "10",
                    "addr:city": "Paris"
                  }
                },
                {
                  "type": "way",
                  "id": 456,
                  "center": {
                    "lat": 48.8567,
                    "lon": 2.3523
                  },
                  "tags": {
                    "name": "Way Hostel",
                    "tourism": "hostel",
                    "beds": "50",
                    "contact:website": "https://wayhostel.com",
                    "internet_access": "wlan",
                    "addr:city": "Paris",
                    "addr:postcode": "75004"
                  }
                },
                {
                  "type": "relation",
                  "id": 789,
                  "center": {
                    "lat": 48.8568,
                    "lon": 2.3524
                  },
                  "tags": {
                    "name": "Relation Resort",
                    "tourism": "resort",
                    "rooms": "200",
                    "breakfast": "included",
                    "wheelchair": "yes"
                  }
                }
              ]
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val provider = OsmPlacesProvider(httpClient)
        val accommodation = provider.getAccommodation(Coordinates(48.8566, 2.3522), 1000)

        assertEquals(3, accommodation.size)

        val nodeHotel = accommodation.find { it.name == "Node Hotel" }
        assertNotNull(nodeHotel)
        assertEquals("osm_node_123", nodeHotel!!.id)
        assertEquals(POIType.ACCOMMODATION, nodeHotel.type)
        assertEquals(AccommodationType.HOTEL, nodeHotel.accommodationType)
        assertEquals(48.8566, nodeHotel.coordinates.lat, 0.0001)
        assertEquals("10 Rue de Rivoli, Paris", nodeHotel.address)
        assertEquals("https://nodehotel.com", nodeHotel.website)
        assertEquals(4, nodeHotel.stars)

        val wayHostel = accommodation.find { it.name == "Way Hostel" }
        assertNotNull(wayHostel)
        assertEquals("osm_way_456", wayHostel!!.id)
        assertEquals(POIType.ACCOMMODATION, wayHostel.type)
        assertEquals(AccommodationType.HOSTEL, wayHostel.accommodationType)
        assertEquals(48.8567, wayHostel.coordinates.lat, 0.0001)
        assertEquals("75004 Paris", wayHostel.address)
        assertEquals("https://wayhostel.com", wayHostel.website)
        assertEquals(50, wayHostel.beds)
        assertEquals(true, wayHostel.amenities.contains("Wi-Fi"))

        val relationResort = accommodation.find { it.name == "Relation Resort" }
        assertNotNull(relationResort)
        assertEquals("osm_relation_789", relationResort!!.id)
        assertEquals(POIType.ACCOMMODATION, relationResort.type)
        assertEquals(AccommodationType.RESORT, relationResort.accommodationType)
        assertEquals(48.8568, relationResort.coordinates.lat, 0.0001)
        assertEquals(200, relationResort.rooms)
        assertEquals(true, relationResort.amenities.contains("Breakfast"))
        assertEquals(true, relationResort.amenities.contains("Wheelchair Accessible"))
    }
    
    @Test
    fun `test incomplete POIs are filtered out`() = runBlocking {
        val mockResponseJson = """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 1,
                  "lat": 48.8,
                  "lon": 2.3
                  // Missing name
                },
                {
                  "type": "node",
                  "id": 2,
                  "tags": {
                    "name": "No Coords"
                  }
                  // Missing lat/lon
                }
              ]
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val provider = OsmPlacesProvider(httpClient)
        val accommodation = provider.getAccommodation(Coordinates(48.8, 2.3), 1000)

        assertEquals(0, accommodation.size)
    }
}
