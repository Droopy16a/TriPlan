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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object AuthRepository {

    private const val GOOGLE_CLIENT_ID =
        "500366235477-s94jq178029b2rpn18ruefgnglrv638v.apps.googleusercontent.com"

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
                AuthState.Authenticated(
                    userId = user?.id ?: "",
                    email = user?.email ?: "",
                    name = meta?.get("full_name")?.toString()?.trim('"') ?: "",
                    avatarUrl = meta?.get("avatar_url")?.toString()?.trim('"')
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
}
