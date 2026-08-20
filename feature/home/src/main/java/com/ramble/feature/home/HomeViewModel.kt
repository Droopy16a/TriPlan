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
import com.ramble.feature.home.util.emojiForDestination
import com.ramble.feature.home.worker.TripPlannerWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
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

    private val prefs = getApplication<Application>().getSharedPreferences("search_form_draft_prefs", android.content.Context.MODE_PRIVATE)

    init {
        // Load draft search form state
        _destinationQuery.value = prefs.getString("draft_destination", "") ?: ""
        _departureQuery.value = prefs.getString("draft_departure", "") ?: ""
        _travelers.value = prefs.getString("draft_travelers", "") ?: ""
        _budget.value = prefs.getString("draft_budget", "") ?: ""
        _preferences.value = prefs.getString("draft_preferences", "") ?: ""
        _startDate.value = prefs.getString("draft_start_date", null)?.let { try { LocalDate.parse(it) } catch(e: Exception) { null } }
        _endDate.value = prefs.getString("draft_end_date", null)?.let { try { LocalDate.parse(it) } catch(e: Exception) { null } }

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
        prefs.edit().putString("draft_destination", query).apply()
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
        prefs.edit().putString("draft_departure", query).apply()
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
        prefs.edit()
            .putString("draft_start_date", start?.toString())
            .putString("draft_end_date", end?.toString())
            .apply()
    }

    fun updateTravelers(value: String) {
        _travelers.value = value
        prefs.edit().putString("draft_travelers", value).apply()
    }

    fun updateBudget(value: String) {
        _budget.value = value
        prefs.edit().putString("draft_budget", value).apply()
    }

    fun updatePreferences(value: String) {
        _preferences.value = value
        prefs.edit().putString("draft_preferences", value).apply()
    }

    fun setSearchFormExpanded(expanded: Boolean) {
        _isSearchFormExpanded.value = expanded
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

            val inputData = workDataOf(
                "departure" to departure,
                "destination" to destination,
                "startDate" to startDate?.toString(),
                "endDate" to endDate?.toString(),
                "travelers" to travelers,
                "budget" to budget,
                "preferences" to preferences
            )

            val workRequest = OneTimeWorkRequestBuilder<TripPlannerWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(getApplication())
                .enqueueUniqueWork("trip_generation", ExistingWorkPolicy.REPLACE, workRequest)

            // Start loading sequence for UI
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

            // Observe the work status
            WorkManager.getInstance(getApplication())
                .getWorkInfoByIdFlow(workRequest.id)
                .collect { workInfo ->
                    when (workInfo?.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            loadingJob.cancel()
                            val tripId = workInfo.outputData.getString("tripId")
                            val trip = tripId?.let { id -> TripRepository.getById(id) }
                            if (trip != null) {
                                _generationState.value = TripGenerationState.Success(trip)
                            }
                        }
                        WorkInfo.State.FAILED -> {
                            loadingJob.cancel()
                            _generationState.value = TripGenerationState.Error("Failed to generate trip. Please check your connection.")
                        }
                        WorkInfo.State.CANCELLED -> {
                            loadingJob.cancel()
                            _generationState.value = TripGenerationState.Cancelled
                        }
                        else -> {}
                    }
                }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        WorkManager.getInstance(getApplication()).cancelUniqueWork("trip_generation")
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
