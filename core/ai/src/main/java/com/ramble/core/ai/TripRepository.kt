package com.ramble.core.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.ramble.core.auth.SupabaseClient
import com.ramble.core.auth.AuthRepository
import com.ramble.core.auth.AuthState
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.collectLatest

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
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    val destination: String,
    val dates: String,
    val travelers: String,
    val budget: String,
    val preferences: String = "",
    val emoji: String = "✈️",
    @SerialName("image_url") val imageUrl: String? = null,
    val itinerary: TripItinerary? = null,
    val expenses: List<Expense> = emptyList(),
    @SerialName("member_names") val memberNames: List<String> = emptyList()
)

/**
 * Supabase-backed trip repository. 
 */
object TripRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val supabase = SupabaseClient.client
    
    private val _trips = MutableStateFlow<List<SavedTrip>>(emptyList())
    val trips: StateFlow<List<SavedTrip>> = _trips.asStateFlow()

    init {
        // Listen to auth state to load/clear trips
        scope.launch {
            AuthRepository.authState.collectLatest { state ->
                if (state is AuthState.Authenticated) {
                    loadTrips()
                } else {
                    _trips.value = emptyList()
                }
            }
        }
    }

    private suspend fun loadTrips() {
        try {
            val result = supabase.postgrest["trips"]
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<SavedTrip>()
            _trips.value = result
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun save(trip: SavedTrip) {
        val authState = AuthRepository.authState.value
        if (authState !is AuthState.Authenticated) return
        
        val tripWithUser = trip.copy(userId = authState.userId)
        
        scope.launch {
            try {
                supabase.postgrest["trips"].upsert(tripWithUser)
                loadTrips()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addExpense(tripId: String, expense: Expense) {
        val trip = getById(tripId) ?: return
        save(trip.copy(expenses = trip.expenses + expense))
    }

    fun deleteExpense(tripId: String, expenseId: String) {
        val trip = getById(tripId) ?: return
        save(trip.copy(expenses = trip.expenses.filterNot { it.id == expenseId }))
    }

    fun updateExpense(tripId: String, updatedExpense: Expense) {
        val trip = getById(tripId) ?: return
        val updatedList = trip.expenses.map {
            if (it.id == updatedExpense.id) updatedExpense else it
        }
        save(trip.copy(expenses = updatedList))
    }

    fun deleteTrip(id: String) {
        scope.launch {
            try {
                supabase.postgrest["trips"].delete {
                    filter {
                        eq("id", id)
                    }
                }
                loadTrips()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getById(id: String): SavedTrip? = _trips.value.find { it.id == id }

    fun clear() {
        _trips.value = emptyList()
    }

    fun getDefaultMembers(travelers: String?): List<String> {
        val count = travelers?.filter { it.isDigit() }?.toIntOrNull() ?: 5
        val list = mutableListOf("Me")
        if (count > 1) {
            list.addAll((1 until count).map { "member $it" })
        }
        return list
    }
}
