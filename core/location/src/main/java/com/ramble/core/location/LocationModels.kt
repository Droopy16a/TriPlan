package com.ramble.core.location

import kotlinx.serialization.Serializable

@Serializable
data class PhotonResponse(
    val features: List<Feature>
)

@Serializable
data class Feature(
    val properties: Properties,
    val geometry: Geometry
)

@Serializable
data class Properties(
    val name: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postcode: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val osm_value: String? = null,
    val osm_id: String? = null,
    val lat: Double? = null,
    val lon: Double? = null
) {
    val displayName: String
        get() = listOfNotNull(city ?: name, state, country)
            .filter { !it.isNullOrBlank() }
            .distinct()
            .joinToString(", ")

    val stableId: String
        get() = osm_id?.toString() ?: name ?: "unknown"
}

@Serializable
data class Geometry(
    val coordinates: List<Double>
)
