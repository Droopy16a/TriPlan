package com.triplane.core.ai

import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Serializable
data class Expense(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val date: String,
    val payer: String = "Me",
    val participants: List<String> = emptyList()
)

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
    val preferences: String = "",
    val emoji: String = "✈️",
    val itinerary: TripItinerary? = null,
    val expenses: List<Expense> = emptyList(),
    val memberNames: List<String> = emptyList()
)

/**
 * In-memory trip repository. In a real app this would persist to Room/DataStore.
 * Exposed as a singleton so both HomeViewModel and the trip screen can access it.
 */
object TripRepository {
    private val defaultKyoto = SavedTrip(
        id = "default-kyoto",
        title = "Kyoto 2026",
        destination = "Kyoto, Japan",
        dates = "Oct 12 - Oct 24",
        travelers = "2",
        budget = "$ 2,500",
        emoji = "⛩️",
        itinerary = null, // Can be null for the static fallback
        memberNames = listOf("Me", "member 1")
    )

    private val _trips = MutableStateFlow<List<SavedTrip>>(listOf(defaultKyoto))
    val trips: StateFlow<List<SavedTrip>> = _trips.asStateFlow()

    fun save(trip: SavedTrip) {
        _trips.update { current ->
            val idx = current.indexOfFirst { it.id == trip.id }
            if (idx >= 0) {
                current.toMutableList().apply { this[idx] = trip }.toList()
            } else {
                listOf(trip) + current
            }
        }
    }

    fun addExpense(tripId: String, expense: Expense) {
        _trips.update { current ->
            current.map { trip ->
                if (trip.id == tripId) {
                    trip.copy(expenses = trip.expenses + expense)
                } else {
                    trip
                }
            }
        }
    }

    fun deleteExpense(tripId: String, expenseId: String) {
        _trips.update { current ->
            current.map { trip ->
                if (trip.id == tripId) {
                    trip.copy(expenses = trip.expenses.filterNot { it.id == expenseId })
                } else {
                    trip
                }
            }
        }
    }

    fun updateExpense(tripId: String, updatedExpense: Expense) {
        _trips.update { current ->
            current.map { trip ->
                if (trip.id == tripId) {
                    val updatedList = trip.expenses.map {
                        if (it.id == updatedExpense.id) updatedExpense else it
                    }
                    trip.copy(expenses = updatedList)
                } else {
                    trip
                }
            }
        }
    }

    fun deleteTrip(id: String) {
        _trips.update { current ->
            current.filterNot { it.id == id }
        }
    }

    fun getById(id: String): SavedTrip? = _trips.value.find { it.id == id }

    fun clear() = _trips.update { emptyList() }

    fun getDefaultMembers(travelers: String?): List<String> {
        val count = travelers?.filter { it.isDigit() }?.toIntOrNull() ?: 5
        val list = mutableListOf("Me")
        if (count > 1) {
            list.addAll((1 until count).map { "member $it" })
        }
        return list
    }
}
