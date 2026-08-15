package com.triplane.core.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object AuthRepository {

    private const val GOOGLE_CLIENT_ID =
        "500366235477-s94jq178029b2rpn18ruefgnglrv638v.apps.googleusercontent.com"
    private const val AVATAR_BUCKET = "avatars"

    private val supabase = SupabaseClient.client

    /**
     * Emits the current authentication state as a [AuthState] Flow.
     * Stays up to date as the session changes (sign-in, token refresh, sign-out).
     */
    val authState: Flow<AuthState> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val user = status.session.user
                val meta = user?.userMetadata
                val firstName = meta.string("first_name")
                val lastName = meta.string("last_name")
                val metadataEmail = meta.string("email")
                val fullName = meta.string("full_name")
                    ?: listOfNotNull(firstName, lastName)
                        .joinToString(" ")
                        .takeIf { it.isNotBlank() }
                AuthState.Authenticated(
                    userId = user?.id ?: "",
                    email = metadataEmail ?: user?.email ?: "",
                    name = fullName ?: "",
                    avatarUrl = meta.string("avatar_url"),
                    birthDate = meta.string("birth_date") ?: "",
                    phoneCountryCode = meta.string("phone_country_code") ?: "",
                    phoneNumber = meta.string("phone_number") ?: ""
                )
            }
            is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
            SessionStatus.LoadingFromStorage -> AuthState.Loading
            SessionStatus.NetworkError -> AuthState.Unauthenticated
        }
    }

    /**
     * Launches the Credential Manager Google Sign-In picker, then authenticates
     * with Supabase using the resulting ID token.
     *
     * @param context An Activity context (required by CredentialManager).
     * @throws Exception on failure; the caller should catch and display an error.
     */
    suspend fun signInWithGoogle(context: Context) {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(GOOGLE_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = context
        )

        val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
        val idToken = googleCredential.idToken

        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
        }
    }

    /**
     * Signs up with email and password using Supabase.
     */
    suspend fun signUpWithEmail(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Signs in with email and password using Supabase.
     */
    suspend fun signInWithEmail(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Updates the signed-in user's Supabase Auth profile metadata.
     *
     * Email is also sent to Supabase Auth as an account email update when it changes.
     * Depending on the project's Supabase settings, that email change may require
     * confirmation before it replaces the canonical auth email.
     */
    suspend fun updateUserInfo(
        firstName: String,
        lastName: String,
        email: String,
        birthDate: String,
        phoneCountryCode: String,
        phoneNumber: String
    ) {
        val trimmedFirstName = firstName.trim()
        val trimmedLastName = lastName.trim()
        val trimmedEmail = email.trim()
        val trimmedBirthDate = birthDate.trim()
        val trimmedPhoneCountryCode = phoneCountryCode.trim()
        val trimmedPhoneNumber = phoneNumber.trim()
        val fullName = listOf(trimmedFirstName, trimmedLastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val currentUser = (supabase.auth.sessionStatus.value as? SessionStatus.Authenticated)
            ?.session
            ?.user
        val currentMetadata = currentUser?.userMetadata

        supabase.auth.updateUser {
            if (trimmedEmail.isNotBlank() && trimmedEmail != currentUser?.email) {
                this.email = trimmedEmail
            }

            data = buildJsonObject {
                currentMetadata?.forEach { (key, value) ->
                    put(key, value)
                }
                put("first_name", trimmedFirstName)
                put("last_name", trimmedLastName)
                put("full_name", fullName)
                put("email", trimmedEmail)
                put("birth_date", trimmedBirthDate)
                put("phone_country_code", trimmedPhoneCountryCode)
                put("phone_number", trimmedPhoneNumber)
            }
        }
    }

    /**
     * Uploads a profile picture to Supabase Storage and stores its public URL
     * in the signed-in user's Supabase Auth metadata.
     */
    suspend fun updateProfilePicture(
        imageBytes: ByteArray,
        contentType: String
    ): String {
        require(imageBytes.isNotEmpty()) {
            "Selected image is empty."
        }

        val session = supabase.auth.currentSessionOrNull()
            ?: error("No authenticated Supabase session found.")
        val user = session.user ?: error("No authenticated Supabase user found.")
        val extension = contentType.fileExtension()
        val objectPath = "${user.id}/profile.$extension"
        val encodedObjectPath = objectPath.storagePathEncode()

        withContext(Dispatchers.IO) {
            val uploadUrl = URL(
                "${SupabaseClient.SUPABASE_URL}/storage/v1/object/$AVATAR_BUCKET/$encodedObjectPath"
            )
            val connection = (uploadUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${session.accessToken}")
                setRequestProperty("Content-Type", contentType)
                setRequestProperty("x-upsert", "true")
            }

            try {
                connection.outputStream.use { it.write(imageBytes) }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val response = runCatching {
                        (connection.errorStream ?: connection.inputStream)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                    }.getOrNull()
                    error(storageUploadErrorMessage(response, responseCode))
                }
            } finally {
                connection.disconnect()
            }
        }

        val publicUrl =
            "${SupabaseClient.SUPABASE_URL}/storage/v1/object/public/$AVATAR_BUCKET/$encodedObjectPath?v=${System.currentTimeMillis()}"

        val currentMetadata = user.userMetadata
        supabase.auth.updateUser {
            data = buildJsonObject {
                currentMetadata?.forEach { (key, value) ->
                    put(key, value)
                }
                put("avatar_url", publicUrl)
            }
        }

        return publicUrl
    }

    /**
     * Signs out from Supabase and clears the local Google credential state.
     */
    suspend fun signOut(context: Context) {
        try {
            supabase.auth.signOut()
        } catch (_: Exception) {}

        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {}
    }

    private fun JsonObject?.string(key: String): String? {
        return this?.get(key)
            ?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
    }

    private fun String.fileExtension(): String {
        return when (substringBefore(";").lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
    }

    private fun String.storagePathEncode(): String {
        return split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
        }
    }

    private fun storageUploadErrorMessage(response: String?, responseCode: Int): String {
        if (response?.contains("NoSuchBucket") == true || response?.contains("Bucket not found") == true) {
            return "Supabase Storage bucket '$AVATAR_BUCKET' was not found. Create a public bucket named '$AVATAR_BUCKET' and allow authenticated users to upload their own profile images."
        }
        if (responseCode == 403 || response?.contains("row-level security", ignoreCase = true) == true) {
            return "Supabase blocked the avatar upload with Storage RLS. Add insert and update policies on storage.objects for authenticated users writing to '$AVATAR_BUCKET/{user_id}/profile.*'."
        }
        return response ?: "Profile picture upload failed with HTTP $responseCode."
    }
}
