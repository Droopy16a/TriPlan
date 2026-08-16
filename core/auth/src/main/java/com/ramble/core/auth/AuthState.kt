package com.ramble.core.auth

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(
        val userId: String,
        val email: String,
        val name: String,
        val avatarUrl: String?,
        val birthDate: String = "",
        val phoneCountryCode: String = "",
        val phoneNumber: String = ""
    ) : AuthState()
}
