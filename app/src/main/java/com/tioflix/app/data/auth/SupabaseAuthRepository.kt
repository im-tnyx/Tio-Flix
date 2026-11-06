package com.tioflix.app.data.auth

import com.tioflix.app.core.config.AppConfig
import com.tioflix.app.domain.repository.AuthRepository
import dagger.Lazy
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import javax.inject.Inject
import javax.inject.Singleton

private const val SignupRedirectUrl = "tioflix://auth"

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val auth: Lazy<Auth>
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        requireConfigured()
        auth.get().signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        nonce: String
    ): Result<Unit> = runCatching {
        requireConfigured()
        check(AppConfig.isGoogleSignInConfigured) {
            "Google Web Client ID is not configured."
        }
        auth.get().signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            this.nonce = nonce
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> = runCatching {
        requireConfigured()
        auth.get().signUpWith(Email, redirectUrl = SignupRedirectUrl) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun resendSignupConfirmation(email: String): Result<Unit> = runCatching {
        requireConfigured()
        auth.get().resendEmail(OtpType.Email.SIGNUP, email)
    }

    override suspend fun sendPasswordReset(email: String, redirectUrl: String): Result<Unit> = runCatching {
        requireConfigured()
        auth.get().resetPasswordForEmail(email = email, redirectUrl = redirectUrl)
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        requireConfigured()
        auth.get().signOut()
    }

    override fun hasSession(): Boolean {
        if (!AppConfig.isSupabaseConfigured) return false
        return auth.get().currentSessionOrNull() != null
    }

    private fun requireConfigured() {
        check(AppConfig.isSupabaseConfigured) {
            "Supabase is not configured. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in local.properties."
        }
    }
}
