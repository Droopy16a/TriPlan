package com.triplane.core.ai

import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    val firstName: String = "Gaetan",
    val lastName: String = "Cozien",
    val email: String = "gaetancozien07@gmail.com",
    val birthDate: String = "",
    val phoneCountryCode: String = "+33",
    val phoneNumber: String = "7 67 13 79 51",
    val travelStyle: String = "Balanced",
    val interests: List<String> = listOf("Culture", "Food", "Nature"),
    val accommodationPreference: List<String> = emptyList(),
    val transportationPreference: List<String> = emptyList(),
    val foodPreferences: List<String> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val notifications: List<Notification> = listOf(
        Notification(
            id = "1",
            title = "Trip ready!",
            message = "Your itinerary for Rome is complete. Check it out now ✨",
            time = "2m ago",
            emoji = "🇮🇹"
        ),
        Notification(
            id = "2",
            title = "New badge earned",
            message = "You've earned the 'Explorer' badge for your 12th trip!",
            time = "1h ago",
            isRead = true,
            emoji = "🏅"
        )
    ),
    val language: String = "English",
    val currency: String = "EUR (€)",
    val units: String = "Metric (km)",
    val theme: String = "Light",
    val avatarUrl: String? = null
) {
    val name: String get() = "$firstName $lastName"
    val unreadNotificationCount: Int get() = notifications.count { !it.isRead }
}

object ProfileRepository {
    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    /**
     * Called right after a successful Google Sign-In to pre-fill
     * the user's name, email, and profile picture from their Google account.
     */
    fun loadFromAuth(name: String, email: String, avatarUrl: String?) {
        val parts = name.trim().split(" ", limit = 2)
        _profile.update {
            it.copy(
                firstName = parts.getOrElse(0) { "" },
                lastName = parts.getOrElse(1) { "" },
                email = email,
                avatarUrl = avatarUrl
            )
        }
    }

    fun updateFirstName(firstName: String) {
        _profile.update { it.copy(firstName = firstName) }
    }

    fun updateLastName(lastName: String) {
        _profile.update { it.copy(lastName = lastName) }
    }

    fun updateEmail(email: String) {
        _profile.update { it.copy(email = email) }
    }

    fun updateBirthDate(date: String) {
        _profile.update { it.copy(birthDate = date) }
    }

    fun updatePhone(countryCode: String, number: String) {
        _profile.update { it.copy(phoneCountryCode = countryCode, phoneNumber = number) }
    }

    fun updateTravelStyle(style: String) {
        _profile.update { it.copy(travelStyle = style) }
    }

    fun toggleInterest(interest: String) {
        _profile.update { current ->
            val newInterests = if (current.interests.contains(interest)) {
                current.interests - interest
            } else {
                current.interests + interest
            }
            current.copy(interests = newInterests)
        }
    }

    fun toggleAccommodation(pref: String) {
        _profile.update { current ->
            val newPrefs = if (current.accommodationPreference.contains(pref)) {
                current.accommodationPreference - pref
            } else {
                current.accommodationPreference + pref
            }
            current.copy(accommodationPreference = newPrefs)
        }
    }

    fun toggleTransportation(pref: String) {
        _profile.update { current ->
            val newPrefs = if (current.transportationPreference.contains(pref)) {
                current.transportationPreference - pref
            } else {
                current.transportationPreference + pref
            }
            current.copy(transportationPreference = newPrefs)
        }
    }

    fun toggleFoodPreference(pref: String) {
        _profile.update { current ->
            val newPrefs = if (current.foodPreferences.contains(pref)) {
                current.foodPreferences - pref
            } else {
                current.foodPreferences + pref
            }
            current.copy(foodPreferences = newPrefs)
        }
    }

    fun markNotificationsAsRead() {
        _profile.update { current ->
            current.copy(notifications = current.notifications.map { it.copy(isRead = true) })
        }
    }

    fun updateLanguage(language: String) {
        _profile.update { it.copy(language = language) }
    }

    fun updateCurrency(currency: String) {
        _profile.update { it.copy(currency = currency) }
    }

    fun updateUnits(units: String) {
        _profile.update { it.copy(units = units) }
    }

    fun updateTheme(theme: String) {
        _profile.update { it.copy(theme = theme) }
    }
}
