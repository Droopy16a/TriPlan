package com.ramble.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ramble.core.ai.AiPlannerService
import com.ramble.core.ai.CommunityTripRepository
import com.ramble.core.ai.LoadingMessages
import com.ramble.core.ai.SavedTrip
import com.ramble.core.ai.TripRepository
import com.ramble.core.location.LocationService
import com.ramble.core.location.Properties
import com.ramble.feature.home.util.PlanningNotificationHelper
import com.ramble.feature.home.util.PlanningSignal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    object Cancelled : TripGenerationState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val aiService = AiPlannerService(application)
    private val locationService = LocationService()
    private val notificationHelper = PlanningNotificationHelper(application)

    private val _generationState = MutableStateFlow<TripGenerationState>(TripGenerationState.Idle)
    val generationState: StateFlow<TripGenerationState> = _generationState

    private val _destinationSuggestions = MutableStateFlow<List<Properties>>(emptyList())
    val destinationSuggestions: StateFlow<List<Properties>> = _destinationSuggestions

    private val _departureSuggestions = MutableStateFlow<List<Properties>>(emptyList())
    val departureSuggestions: StateFlow<List<Properties>> = _departureSuggestions

    private val _destinationQuery = MutableStateFlow("")
    val destinationQuery: StateFlow<String> = _destinationQuery

    private val _departureQuery = MutableStateFlow("")
    val departureQuery: StateFlow<String> = _departureQuery

    private val _startDate = MutableStateFlow<LocalDate?>(null)
    val startDate: StateFlow<LocalDate?> = _startDate

    private val _endDate = MutableStateFlow<LocalDate?>(null)
    val endDate: StateFlow<LocalDate?> = _endDate

    private val _travelers = MutableStateFlow("")
    val travelers: StateFlow<String> = _travelers

    private val _budget = MutableStateFlow("")
    val budget: StateFlow<String> = _budget

    private val _preferences = MutableStateFlow("")
    val preferences: StateFlow<String> = _preferences

    private val _isSearchFormExpanded = MutableStateFlow(false)
    val isSearchFormExpanded: StateFlow<Boolean> = _isSearchFormExpanded

    private var suggestionJob: Job? = null
    private var generationJob: Job? = null

    // Expose the list of saved trips (refreshes when a new one is saved)
    val savedTrips: StateFlow<List<SavedTrip>> = TripRepository.trips
    val loadingTrips: StateFlow<Boolean> = TripRepository.loadingTrips

    private val _selectedCityProperties = MutableStateFlow<Properties?>(null)
    val selectedCityProperties: StateFlow<Properties?> = _selectedCityProperties

    private val _exploreSearchQuery = MutableStateFlow("")
    val exploreSearchQuery: StateFlow<String> = _exploreSearchQuery

    val exploreSearchResults: StateFlow<List<SavedTrip>> = combine(
        _exploreSearchQuery,
        CommunityTripRepository.communityTrips
    ) { query, trips ->
        val lowerQuery = query.trim().lowercase()
        if (lowerQuery.isEmpty()) trips
        else trips.filter { trip ->
            trip.destination.lowercase().contains(lowerQuery) ||
            trip.title.lowercase().contains(lowerQuery)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CommunityTripRepository.communityTrips.value)

    init {
        viewModelScope.launch {
            PlanningSignal.cancelSignal.collectLatest {
                cancelGeneration()
            }
        }
    }

    fun selectCity(properties: Properties) {
        _selectedCityProperties.value = properties
        updateDestinationQuery(properties.displayName, triggerSuggestions = false)
    }

    fun clearSelection() {
        _selectedCityProperties.value = null
    }

    fun updateDestinationQuery(query: String, triggerSuggestions: Boolean = true) {
        _destinationQuery.value = query
        if (triggerSuggestions) {
            updateDestinationSuggestions(query)
        } else {
            suggestionJob?.cancel()
            _destinationSuggestions.value = emptyList()
        }
    }

    fun updateExploreSearchQuery(query: String) {
        _exploreSearchQuery.value = query
    }

    fun updateDepartureQuery(query: String, triggerSuggestions: Boolean = true) {
        _departureQuery.value = query
        if (triggerSuggestions) {
            updateDepartureSuggestions(query)
        } else {
            suggestionJob?.cancel()
            _departureSuggestions.value = emptyList()
        }
    }

    fun updateDateRange(start: LocalDate?, end: LocalDate?) {
        _startDate.value = start
        _endDate.value = end
    }

    fun updateTravelers(value: String) {
        _travelers.value = value
    }

    fun updateBudget(value: String) {
        _budget.value = value
    }

    fun updatePreferences(value: String) {
        _preferences.value = value
    }

    fun setSearchFormExpanded(expanded: Boolean) {
        _isSearchFormExpanded.value = expanded
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
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _generationState.value = TripGenerationState.Loading("Planning your trip…")
            notificationHelper.showNotification("Planning your trip…")

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
                    notificationHelper.showNotification(msg)
                    delay(2800)
                    if (index < sequence.size) index++
                }
            }

            val result = aiService.generateTrip(departure, destination, startDate, endDate, travelers, budget, preferences)
            loadingJob.cancel()
            notificationHelper.dismissNotification()

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

                val cityName = destination.substringBefore(",").trim()
                val year = startDate?.year ?: LocalDate.now().year
                val defaultTitle = "$cityName $year"

                val trip = SavedTrip(
                    id = UUID.randomUUID().toString(),
                    title = defaultTitle,
                    destination = itinerary.destination,
                    dates = durationText,
                    travelers = travelers,
                    budget = budget,
                    preferences = preferences,
                    emoji = emojiForDestination(destination),
                    itinerary = itinerary
                )
                TripRepository.save(trip)
                _generationState.value = TripGenerationState.Success(trip)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                _generationState.value = TripGenerationState.Error(msg)
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _generationState.value = TripGenerationState.Cancelled
        notificationHelper.dismissNotification()
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
