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
import kotlinx.coroutines.flow.collectLatest

@Serializable
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean = false,
    val emoji: String = "🔔"
)

@Serializable
data class UserProfile(
    val id: String? = null,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val email: String = "",
    @SerialName("birth_date") val birthDate: String? = "",
    @SerialName("phone_country_code") val phoneCountryCode: String? = "",
    @SerialName("phone_number") val phoneNumber: String? = "",
    @SerialName("travel_style") val travelStyle: String = "Balanced",
    val interests: List<String> = emptyList(),
    @SerialName("accommodation_preference") val accommodationPreference: List<String> = emptyList(),
    @SerialName("transportation_preference") val transportationPreference: List<String> = emptyList(),
    @SerialName("food_preferences") val foodPreferences: List<String> = emptyList(),
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
    val notifications: List<Notification> = emptyList(),
    val language: String = "English",
    val currency: String = "EUR (€)",
    val units: String = "Metric (km)",
    val theme: String = "Light",
    @SerialName("avatar_url") val avatarUrl: String? = null
) {
    val name: String get() = "$firstName $lastName"
    val unreadNotificationCount: Int get() = notifications.count { !it.isRead }
}

object ProfileRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val supabase = SupabaseClient.client

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    init {
        scope.launch {
            AuthRepository.authState.collectLatest { state ->
                if (state is AuthState.Authenticated) {
                    // 1. First, apply metadata from AuthState (immediate source)
                    val parts = state.name.trim().split(" ", limit = 2)
                    _profile.update { current ->
                        current.copy(
                            id = state.userId,
                            firstName = parts.getOrElse(0) { "" },
                            lastName = parts.getOrElse(1) { "" },
                            email = state.email,
                            avatarUrl = state.avatarUrl,
                            birthDate = state.birthDate,
                            phoneCountryCode = state.phoneCountryCode,
                            phoneNumber = state.phoneNumber
                        )
                    }
                    // 2. Then load extended preferences from Database
                    loadFromDatabase(state.userId)
                } else {
                    _profile.value = UserProfile()
                }
            }
        }
    }

    private suspend fun loadFromDatabase(userId: String) {
        try {
            val result = supabase.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
            
            if (result != null) {
                // Merge DB results into current state, but keep the core Auth metadata
                // as the definitive source for name/email/avatar to avoid stale overwrites
                _profile.update { current ->
                    result.copy(
                        id = current.id,
                        firstName = current.firstName,
                        lastName = current.lastName,
                        email = current.email,
                        avatarUrl = current.avatarUrl,
                        birthDate = current.birthDate,
                        phoneCountryCode = current.phoneCountryCode,
                        phoneNumber = current.phoneNumber
                    )
                }
            } else {
                // Create initial profile in DB if it doesn't exist
                saveToDatabase(_profile.value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToDatabase(profile: UserProfile) {
        scope.launch {
            try {
                supabase.postgrest["profiles"].upsert(profile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadFromAuth(
        name: String,
        email: String,
        avatarUrl: String?,
        birthDate: String = "",
        phoneCountryCode: String = "",
        phoneNumber: String = ""
    ) {
        // Core info is handled by AuthRepository and authState observer
    }

    fun updateAccountInfo(
        firstName: String,
        lastName: String,
        email: String,
        birthDate: String,
        phoneCountryCode: String,
        phoneNumber: String
    ) {
        // Core info is updated via AuthRepository; this repository will see the changes via authState
    }

    fun updateAvatarUrl(avatarUrl: String?) {
        // Core info is updated via AuthRepository
    }

    fun updateTravelStyle(style: String) {
        val updated = _profile.value.copy(travelStyle = style)
        _profile.value = updated
        saveToDatabase(updated)
    }

    fun toggleInterest(interest: String) {
        val current = _profile.value
        val newInterests = if (current.interests.contains(interest)) {
            current.interests - interest
        } else {
            current.interests + interest
        }
        val updated = current.copy(interests = newInterests)
        _profile.value = updated
        saveToDatabase(updated)
    }

    fun toggleAccommodation(pref: String) {
        val current = _profile.value
        val newPrefs = if (current.accommodationPreference.contains(pref)) {
            current.accommodationPreference - pref
        } else {
            current.accommodationPreference + pref
        }
        val updated = current.copy(accommodationPreference = newPrefs)
        _profile.value = updated
        saveToDatabase(updated)
    }

    fun toggleTransportation(pref: String) {
        val current = _profile.value
        val newPrefs = if (current.transportationPreference.contains(pref)) {
            current.transportationPreference - pref
        } else {
            current.transportationPreference + pref
        }
        val updated = current.copy(transportationPreference = newPrefs)
        _profile.value = updated
        saveToDatabase(updated)
    }

    fun toggleFoodPreference(pref: String) {
        val current = _profile.value
        val newPrefs = if (current.foodPreferences.contains(pref)) {
            current.foodPreferences - pref
        } else {
            current.foodPreferences + pref
        }
        val updated = current.copy(foodPreferences = newPrefs)
        _profile.value = updated
        saveToDatabase(updated)
    }

    fun updateLanguage(language: String) {
        val updated = _profile.value.copy(language = language)
        _profile.value = updated
        saveToDatabase(updated)
    }

    fun updateCurrency(currency: String) {
        val updated = _profile.value.copy(currency = currency)
        _profile.value = updated
        saveToDatabase(updated)
    }

    fun updateUnits(units: String) {
        val updated = _profile.value.copy(units = units)
        _profile.value = updated
        saveToDatabase(updated)
    }

    fun updateTheme(theme: String) {
        val updated = _profile.value.copy(theme = theme)
        _profile.value = updated
        saveToDatabase(updated)
    }

    fun markNotificationsAsRead() {
        _profile.update { current ->
            current.copy(notifications = current.notifications.map { it.copy(isRead = true) })
        }
    }
}
