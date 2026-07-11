package com.tioflix.app.data.auth

import com.tioflix.app.core.config.AppConfig
import com.tioflix.app.domain.repository.AuthRepository
import dagger.Lazy
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import javax.inject.Inject
import javax.inject.Singleton

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

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> = runCatching {
        requireConfigured()
        auth.get().signUpWith(Email) {
            this.email = email
            this.password = password
        }
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
