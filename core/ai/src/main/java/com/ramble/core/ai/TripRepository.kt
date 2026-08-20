package com.ramble.core.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
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
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.collectLatest
import android.util.Log

@Serializable
data class Expense(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val date: String,
    val payer: String = "Me",
    val participants: List<String> = emptyList(),
    val isSettlement: Boolean = false
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
    val dates: String? = "",
    val travelers: String? = "",
    val budget: String? = "",
    val preferences: String? = "",
    val emoji: String? = "✈️",
    @SerialName("image_url") val imageUrl: String? = null,
    val itinerary: TripItinerary? = null,
    val expenses: List<Expense>? = emptyList(),
    @Transient val memberNames: List<String>? = emptyList(),
    @Transient val memberAvatarUrls: Map<String, String>? = emptyMap()
)

@Serializable
data class TripMemberRow(
    @SerialName("trip_id") val tripId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("joined_at") val joinedAt: String? = null
)

@Serializable
private data class TripMemberUpsert(
    @SerialName("trip_id") val tripId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
private data class JoinTripParams(
    @SerialName("p_trip_id") val tripId: String
)

@Serializable
data class TripMemberProfile(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null
)

@Serializable
data class UserProfileRow(
    val id: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .trim()
            .ifBlank { email.orEmpty() }
}

@Serializable
private data class UserProfileUpsert(
    val id: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val email: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null
)

/**
 * Supabase-backed trip repository. 
 */
object TripRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val supabase = SupabaseClient.client
    @Volatile private var appContext: android.content.Context? = null
    
    private val _trips = MutableStateFlow<List<SavedTrip>>(emptyList())
    val trips: StateFlow<List<SavedTrip>> = _trips.asStateFlow()
    private val _loadingTrips = MutableStateFlow(false)
    val loadingTrips: StateFlow<Boolean> = _loadingTrips.asStateFlow()
    private val _tripMemberProfiles = MutableStateFlow<Map<String, List<TripMemberProfile>>>(emptyMap())
    val tripMemberProfiles: StateFlow<Map<String, List<TripMemberProfile>>> = _tripMemberProfiles.asStateFlow()
    private val placeholderRegex = Regex("^member\\s+\\d+$", RegexOption.IGNORE_CASE)

    // TTL Cache for Trip Member Profiles (10 minutes TTL)
    private val profileCache = java.util.concurrent.ConcurrentHashMap<String, Pair<UserProfileRow?, Long>>()
    private val PROFILE_TTL_MS = 10 * 60 * 1000L

    fun initContext(context: android.content.Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            loadFromDiskCache()
        }
    }

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun loadFromDiskCache() {
        val ctx = appContext ?: return
        try {
            val file = java.io.File(ctx.filesDir, "saved_trips_cache.json")
            if (file.exists()) {
                val jsonStr = file.readText()
                if (jsonStr.isNotBlank()) {
                    val cached = json.decodeFromString<List<SavedTrip>>(jsonStr)
                    if (cached.isNotEmpty() && _trips.value.isEmpty()) {
                        _trips.value = cached
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TripRepository", "Error loading disk cache", e)
        }
    }

    private fun saveToDiskCache(trips: List<SavedTrip>) {
        val ctx = appContext ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(ctx.filesDir, "saved_trips_cache.json")
                val jsonStr = json.encodeToString(trips)
                file.writeText(jsonStr)
            } catch (e: Exception) {
                Log.e("TripRepository", "Error saving disk cache", e)
            }
        }
    }

    init {
        // Listen to auth state to load/clear trips
        scope.launch {
            AuthRepository.authState.collectLatest { state ->
                if (state is AuthState.Authenticated) {
                    loadFromDiskCache()
                    loadTrips()
                } else {
                    _trips.value = emptyList()
                    _tripMemberProfiles.value = emptyMap()
                }
            }
        }
    }

    private suspend fun loadTrips() {
        val authState = AuthRepository.authState.value
        if (authState !is AuthState.Authenticated) return

        _loadingTrips.value = true
        try {
            syncOwnProfile(authState)
            val ownedTrips = supabase.postgrest["trips"]
                .select {
                    filter {
                        eq("user_id", authState.userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<SavedTrip>()

            val joinedTripIds = supabase.postgrest["trip_members"]
                .select {
                    filter {
                        eq("user_id", authState.userId)
                    }
                }
                .decodeList<TripMemberRow>()
                .map { it.tripId }
                .distinct()

            val ownedIds = ownedTrips.map { it.id }.toSet()
            val joinedTrips = joinedTripIds
                .filterNot { it in ownedIds }
                .mapNotNull { fetchTripDirectly(it) }

            val result = (ownedTrips + joinedTrips).distinctBy { it.id }
            _trips.value = result
            saveToDiskCache(result)
            loadMembersForTrips(result.map { it.id })
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _loadingTrips.value = false
        }
    }

@Serializable
private data class TripUpdate(
    val title: String,
    val destination: String,
    val dates: String?,
    val travelers: String?,
    val budget: String?,
    val preferences: String?,
    val emoji: String?,
    @SerialName("image_url") val imageUrl: String?,
    val itinerary: TripItinerary?,
    val expenses: List<Expense>?
)

    fun save(trip: SavedTrip) {
        val authState = AuthRepository.authState.value
        if (authState !is AuthState.Authenticated) return
        val currentUserName = currentUserDisplayName(authState)
        val memberNames = buildDisplayMembers(
            existingMembers = trip.memberNames.orEmpty(),
            travelers = trip.travelers,
            currentUserName = currentUserName,
            includeCurrentUser = true
        )
        
        val tripWithUser = trip.copy(
            userId = trip.userId ?: authState.userId,
            memberNames = memberNames,
            memberAvatarUrls = mergeMemberAvatarUrls(
                existingAvatarUrls = trip.memberAvatarUrls.orEmpty(),
                memberNames = memberNames,
                currentUserName = currentUserName,
                currentUserAvatarUrl = authState.avatarUrl
            )
        )

        _trips.update { currentTrips ->
            val updated = listOf(tripWithUser) + currentTrips.filterNot { it.id == tripWithUser.id }
            saveToDiskCache(updated)
            updated
        }
        
        scope.launch {
            saveToRemote(tripWithUser, authState)
        }
    }

    suspend fun saveSuspend(trip: SavedTrip) {
        val authState = AuthRepository.authState.value
        if (authState !is AuthState.Authenticated) return
        val currentUserName = currentUserDisplayName(authState)
        val memberNames = buildDisplayMembers(
            existingMembers = trip.memberNames.orEmpty(),
            travelers = trip.travelers,
            currentUserName = currentUserName,
            includeCurrentUser = true
        )
        
        val tripWithUser = trip.copy(
            userId = trip.userId ?: authState.userId,
            memberNames = memberNames,
            memberAvatarUrls = mergeMemberAvatarUrls(
                existingAvatarUrls = trip.memberAvatarUrls.orEmpty(),
                memberNames = memberNames,
                currentUserName = currentUserName,
                currentUserAvatarUrl = authState.avatarUrl
            )
        )

        _trips.update { currentTrips ->
            val updated = listOf(tripWithUser) + currentTrips.filterNot { it.id == tripWithUser.id }
            saveToDiskCache(updated)
            updated
        }
        
        saveToRemote(tripWithUser, authState)
    }

    private suspend fun saveToRemote(tripWithUser: SavedTrip, authState: AuthState.Authenticated) {
        try {
            // If we are not the owner, use update instead of upsert to avoid violating INSERT RLS policies
            if (tripWithUser.userId != null && tripWithUser.userId != authState.userId) {
                val updateData = TripUpdate(
                    title = tripWithUser.title,
                    destination = tripWithUser.destination,
                    dates = tripWithUser.dates,
                    travelers = tripWithUser.travelers,
                    budget = tripWithUser.budget,
                    preferences = tripWithUser.preferences,
                    emoji = tripWithUser.emoji,
                    imageUrl = tripWithUser.imageUrl,
                    itinerary = tripWithUser.itinerary,
                    expenses = tripWithUser.expenses
                )
                supabase.postgrest["trips"].update(updateData) {
                    filter {
                        eq("id", tripWithUser.id)
                    }
                }
            } else {
                supabase.postgrest["trips"].upsert(tripWithUser)
            }
            ensureTripMember(tripWithUser.id, authState)
            loadTrips()
        } catch (e: Exception) {
            Log.e("TripRepository", "Failed to save trip ${tripWithUser.id}", e)
            e.printStackTrace()
        }
    }

    fun addExpense(tripId: String, expense: Expense) {
        val trip = getById(tripId) ?: return
        save(trip.copy(expenses = trip.expenses.orEmpty() + expense))
    }

    fun deleteExpense(tripId: String, expenseId: String) {
        val trip = getById(tripId) ?: return
        save(trip.copy(expenses = trip.expenses.orEmpty().filterNot { it.id == expenseId }))
    }

    fun updateExpense(tripId: String, updatedExpense: Expense) {
        val trip = getById(tripId) ?: return
        val updatedList = trip.expenses.orEmpty().map {
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

    fun leaveTrip(tripId: String) {
        val authState = AuthRepository.authState.value
        if (authState !is AuthState.Authenticated) return

        scope.launch {
            try {
                supabase.postgrest["trip_members"].delete {
                    filter {
                        eq("trip_id", tripId)
                        eq("user_id", authState.userId)
                    }
                }
                loadTrips()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getById(id: String): SavedTrip? = _trips.value.find { it.id == id }

    fun getNextTrip(): SavedTrip? {
        val now = java.time.LocalDate.now()
        return _trips.value
            .filter { trip ->
                val startDateStr = trip.itinerary?.days?.firstOrNull()?.date
                if (startDateStr == null) false
                else {
                    try {
                        val startDate = java.time.LocalDate.parse(startDateStr)
                        !startDate.isBefore(now)
                    } catch (_: Exception) {
                        false
                    }
                }
            }
            .minByOrNull { trip ->
                val startDateStr = trip.itinerary?.days?.firstOrNull()?.date!!
                java.time.LocalDate.parse(startDateStr).toEpochDay()
            }
    }

    suspend fun fetchTripDirectly(tripId: String): SavedTrip? {
        return try {
            supabase.postgrest["trips"].select {
                filter {
                    eq("id", tripId)
                }
            }.decodeSingleOrNull<SavedTrip>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun joinTrip(trip: SavedTrip): String? {
        return joinTripById(trip.id)
    }

    suspend fun joinTripById(tripId: String): String? {
        val authState = AuthRepository.authState.value
        if (authState !is AuthState.Authenticated) return null

        try {
            ensureTripMember(tripId, authState)
            loadTrips()
            return tripId
        } catch (e: Exception) {
            Log.e("TripRepository", "Failed to join trip", e)
            return null
        }
    }

    suspend fun refreshTrips() {
        loadTrips()
    }

    fun memberAvatarUrlsForTrip(trip: SavedTrip?): Map<String, String> {
        val tripId = trip?.id
        val profileAvatars = if (tripId != null) {
            _tripMemberProfiles.value[tripId]
                .orEmpty()
                .mapNotNull { profile ->
                    profile.avatarUrl
                        ?.takeIf { it.isNotBlank() }
                        ?.let { profile.displayName to it }
                }
                .toMap()
        } else {
            emptyMap()
        }
        if (profileAvatars.isNotEmpty()) return profileAvatars

        val authState = AuthRepository.authState.value
        return mergeMemberAvatarUrls(
            existingAvatarUrls = trip?.memberAvatarUrls.orEmpty(),
            memberNames = membersForTrip(trip),
            currentUserName = currentUserDisplayName(authState),
            currentUserAvatarUrl = (authState as? AuthState.Authenticated)?.avatarUrl
        )
    }

    fun membersForTrip(trip: SavedTrip?): List<String> {
        val profileMembers = trip?.id
            ?.let { _tripMemberProfiles.value[it] }
            .orEmpty()
            .map { it.displayName.ifBlank { "Member" } }

        if (profileMembers.isNotEmpty()) {
            return buildDisplayMembers(
                existingMembers = profileMembers,
                travelers = trip?.travelers,
                currentUserName = currentUserDisplayName(AuthRepository.authState.value),
                includeCurrentUser = true
            )
        }

        return buildDisplayMembers(
            existingMembers = trip?.memberNames.orEmpty(),
            travelers = trip?.travelers,
            currentUserName = currentUserDisplayName(AuthRepository.authState.value),
            includeCurrentUser = true
        )
    }

    fun clear() {
        _trips.value = emptyList()
    }

    fun getDefaultMembers(travelers: String?): List<String> {
        return buildDisplayMembers(
            existingMembers = emptyList(),
            travelers = travelers,
            currentUserName = currentUserDisplayName(AuthRepository.authState.value),
            includeCurrentUser = true
        )
    }

    fun reconcileMembersForTravelers(
        existingMembers: List<String>?,
        travelers: String?
    ): List<String> {
        return buildDisplayMembers(
            existingMembers = existingMembers.orEmpty(),
            travelers = travelers,
            currentUserName = currentUserDisplayName(AuthRepository.authState.value),
            includeCurrentUser = true
        )
    }

    private fun buildDisplayMembers(
        existingMembers: List<String>,
        travelers: String?,
        currentUserName: String,
        includeCurrentUser: Boolean
    ): List<String> {
        val travelerCount = travelerCount(travelers)
        val realMembers = existingMembers
            .map { it.trim() }
            .filter { it.isNotBlank() && !isPlaceholderMember(it) }
            .distinctBy { it.lowercase() }
            .toMutableList()

        if (includeCurrentUser) {
            val userIndex = realMembers.indexOfFirst { it.equals(currentUserName, ignoreCase = true) }
            if (userIndex != -1) {
                val name = realMembers.removeAt(userIndex)
                realMembers.add(0, name)
            } else {
                realMembers.add(0, currentUserName)
            }
        }

        val displayMembers = realMembers.take(travelerCount.coerceAtLeast(realMembers.size)).toMutableList()
        var placeholderNumber = 1
        while (displayMembers.size < travelerCount) {
            val placeholder = "Member $placeholderNumber"
            if (displayMembers.none { it.equals(placeholder, ignoreCase = true) }) {
                displayMembers.add(placeholder)
            }
            placeholderNumber++
        }
        return displayMembers
    }

    private suspend fun ensureTripMember(tripId: String, authState: AuthState.Authenticated) {
        syncOwnProfile(authState)
        supabase.postgrest.rpc("join_trip", JoinTripParams(tripId))
    }

    private suspend fun syncOwnProfile(authState: AuthState.Authenticated) {
        supabase.postgrest["profiles"].upsert(
            UserProfileUpsert(
                id = authState.userId,
                firstName = authState.name.trim().split(" ", limit = 2).getOrElse(0) { "" },
                lastName = authState.name.trim().split(" ", limit = 2).getOrElse(1) { "" },
                email = authState.email,
                avatarUrl = authState.avatarUrl
            )
        )
    }

    private suspend fun loadMembersForTrips(tripIds: List<String>) {
        if (tripIds.isEmpty()) {
            _tripMemberProfiles.value = emptyMap()
            return
        }

        val membersByTrip = tripIds.associateWith { tripId ->
            try {
                supabase.postgrest["trip_members"]
                    .select {
                        filter {
                            eq("trip_id", tripId)
                        }
                    }
                    .decodeList<TripMemberRow>()
            } catch (e: Exception) {
                Log.e("TripRepository", "Failed to load members for trip $tripId", e)
                emptyList()
            }
        }

        val profileIds = membersByTrip.values
            .flatten()
            .map { it.userId }
            .distinct()

        val now = System.currentTimeMillis()
        val profileIdsToFetch = profileIds.filter { userId ->
            val cached = profileCache[userId]
            cached == null || (now - cached.second) > PROFILE_TTL_MS
        }

        val newlyFetchedProfiles = profileIdsToFetch.associateWith { userId ->
            try {
                val profile = supabase.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingleOrNull<UserProfileRow>()
                profileCache[userId] = Pair(profile, now)
                profile
            } catch (e: Exception) {
                Log.e("TripRepository", "Failed to load profile $userId", e)
                null
            }
        }

        val profilesById = profileIds.associateWith { userId ->
            if (profileCache.containsKey(userId)) {
                profileCache[userId]?.first
            } else {
                newlyFetchedProfiles[userId]
            }
        }

        _tripMemberProfiles.value = membersByTrip.mapValues { (_, rows) ->
            rows.map { row ->
                val profile = profilesById[row.userId]
                TripMemberProfile(
                    userId = row.userId,
                    displayName = profile?.displayName
                        ?.takeIf { it.isNotBlank() }
                        ?: "Member",
                    avatarUrl = profile?.avatarUrl
                )
            }
        }
    }

    private fun travelerCount(travelers: String?): Int {
        return travelers
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
            ?.coerceAtLeast(1)
            ?: 1
    }

    private fun isPlaceholderMember(name: String): Boolean {
        return name.equals("Me", ignoreCase = true) || placeholderRegex.matches(name.trim())
    }

    private fun currentUserDisplayName(authState: AuthState): String {
        return if (authState is AuthState.Authenticated) {
            authState.name.ifBlank { authState.email.ifBlank { "You" } }
        } else {
            "You"
        }
    }

    private fun mergeMemberAvatarUrls(
        existingAvatarUrls: Map<String, String>,
        memberNames: List<String>,
        currentUserName: String,
        currentUserAvatarUrl: String?
    ): Map<String, String> {
        val normalizedMembers = memberNames.associateBy { it.lowercase() }
        val avatarUrls = existingAvatarUrls
            .mapKeys { (name, _) -> normalizedMembers[name.trim().lowercase()] ?: name.trim() }
            .filterKeys { name -> memberNames.any { it.equals(name, ignoreCase = true) } }
            .filterValues { it.isNotBlank() }
            .toMutableMap()

        if (!currentUserAvatarUrl.isNullOrBlank()) {
            val displayName = memberNames.firstOrNull { it.equals(currentUserName, ignoreCase = true) }
                ?: currentUserName
            avatarUrls[displayName] = currentUserAvatarUrl
        }

        return avatarUrls
    }
}
