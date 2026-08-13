package com.triplane.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.triplane.core.ai.AiPlannerService
import com.triplane.core.ai.LoadingMessages
import com.triplane.core.ai.SavedTrip
import com.triplane.core.ai.TripRepository
import com.triplane.core.location.LocationService
import com.triplane.core.location.Properties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

sealed class TripGenerationState {
    object Idle : TripGenerationState()
    data class Loading(val message: String = "Planning your trip…") : TripGenerationState()
    data class Success(val trip: SavedTrip) : TripGenerationState()
    data class Error(val message: String) : TripGenerationState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val aiService = AiPlannerService(application)
    private val locationService = LocationService()

    private val _generationState = MutableStateFlow<TripGenerationState>(TripGenerationState.Idle)
    val generationState: StateFlow<TripGenerationState> = _generationState

    private val _destinationSuggestions = MutableStateFlow<List<Properties>>(emptyList())
    val destinationSuggestions: StateFlow<List<Properties>> = _destinationSuggestions

    private val _departureSuggestions = MutableStateFlow<List<Properties>>(emptyList())
    val departureSuggestions: StateFlow<List<Properties>> = _departureSuggestions

    private var suggestionJob: Job? = null

    // Expose the list of saved trips (refreshes when a new one is saved)
    private val _savedTrips = MutableStateFlow<List<SavedTrip>>(TripRepository.trips)
    val savedTrips: StateFlow<List<SavedTrip>> = _savedTrips

    private val _selectedCityProperties = MutableStateFlow<Properties?>(null)
    val selectedCityProperties: StateFlow<Properties?> = _selectedCityProperties

    fun selectCity(properties: Properties) {
        _selectedCityProperties.value = properties
    }

    fun clearSelection() {
        _selectedCityProperties.value = null
    }

    private fun emojiForDestination(destination: String): String {
        val lower = destination.lowercase()
        return when {
            lower.contains("japan") || lower.contains("tokyo") || lower.contains("kyoto") -> "⛩️"
            lower.contains("paris") || lower.contains("france") -> "🗼"
            lower.contains("new york") || lower.contains("usa") || lower.contains("america") -> "🗽"
            lower.contains("london") || lower.contains("uk") || lower.contains("england") -> "🎡"
            lower.contains("rome") || lower.contains("italy") -> "🏛️"
            lower.contains("barcelona") || lower.contains("spain") -> "💃"
            lower.contains("dubai") -> "🏙️"
            lower.contains("bali") || lower.contains("indonesia") -> "🌴"
            else -> "✈️"
        }
    }

    fun generateTrip(
        departure: String,
        destination: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        travelers: String,
        budget: String,
        preferences: String
    ) {
        viewModelScope.launch {
            _generationState.value = TripGenerationState.Loading("Planning your trip…")

            // Start loading sequence
            val loadingJob = launch {
                val sequence = listOf(
                    { LoadingMessages.getRandom(LoadingMessages.tripPlanning) },
                    { "Understanding your preferences…" },
                    { "Exploring the best of $destination…" },
                    { LoadingMessages.getRandom(LoadingMessages.discoveringDestination) },
                    { "Balancing your $budget budget…" },
                    { LoadingMessages.getRandom(LoadingMessages.logistics) },
                    { LoadingMessages.getRandom(LoadingMessages.groupTrips) },
                    { LoadingMessages.getRandom(LoadingMessages.foodAndExperiences) },
                    { "Checking travel times…" },
                    { LoadingMessages.getRandom(LoadingMessages.finalTouches) },
                    { "Your trip is ready ✨" }
                )

                var index = 0
                while (true) {
                    val msg = if (index < sequence.size) {
                        sequence[index]()
                    } else {
                        LoadingMessages.getRandom(LoadingMessages.playful)
                    }
                    _generationState.value = TripGenerationState.Loading(msg)
                    delay(2800)
                    if (index < sequence.size) index++
                }
            }

            val result = aiService.generateTrip(departure, destination, startDate, endDate, travelers, budget, preferences)
            loadingJob.cancel()

            if (result.isSuccess) {
                val itinerary = result.getOrThrow()
                
                // Duration calculation in Kotlin for UI display
                val durationText = if (startDate != null && endDate != null) {
                    val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
                    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                    "${startDate.format(formatter)} – ${endDate.format(formatter)} ($days days)"
                } else {
                    "flexible"
                }

                val trip = SavedTrip(
                    id = UUID.randomUUID().toString(),
                    title = itinerary.title,
                    destination = itinerary.destination,
                    dates = durationText,
                    travelers = travelers,
                    budget = budget,
                    emoji = emojiForDestination(destination),
                    itinerary = itinerary
                )
                TripRepository.save(trip)
                _savedTrips.value = TripRepository.trips
                _generationState.value = TripGenerationState.Success(trip)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                _generationState.value = TripGenerationState.Error(msg)
            }
        }
    }

    fun resetState() {
        _generationState.value = TripGenerationState.Idle
    }

    fun updateDestinationSuggestions(query: String) {
        suggestionJob?.cancel()
        if (query.isBlank()) {
            _destinationSuggestions.value = emptyList()
            return
        }

        suggestionJob = viewModelScope.launch {
            delay(300)
            _destinationSuggestions.value = locationService.getAutocompleteSuggestions(query)
        }
    }

    fun updateDepartureSuggestions(query: String) {
        suggestionJob?.cancel()
        if (query.isBlank()) {
            _departureSuggestions.value = emptyList()
            return
        }

        suggestionJob = viewModelScope.launch {
            delay(300)
            _departureSuggestions.value = locationService.getAutocompleteSuggestions(query)
        }
    }
}
