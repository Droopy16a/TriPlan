package com.ramble.core.ai

import kotlinx.serialization.Serializable

@Serializable
data class TripItinerary(
    val destination: String,
    val title: String,
    val summary: String,
    val budgetAllocation: BudgetAllocation,
    val days: List<TripDay>,
    val estimatedTotalCost: Double? = null
)

@Serializable
data class BudgetAllocation(
    val accommodation: String,
    val food: String,
    val transport: String,
    val activities: String
)

@Serializable
data class TripDay(
    val dayNumber: Int,
    val date: String,        // "YYYY-MM-DD" — required
    val theme: String,
    val temperature: String? = null,
    val weatherCondition: String? = null,
    val steps: List<TripStep>
)

@Serializable
data class TripStep(
    val time: String,
    val title: String,
    val description: String,
    val category: String, // e.g. "Food", "Activity", "Transport", "FreeTime", "Accommodation"
    val estimatedCost: Double? = null,
    val lat: Double? = null,
    val lon: Double? = null
)
