package com.tioflix.app.data.auth

import com.tioflix.app.domain.model.PasswordRecoveryLink
import com.tioflix.app.domain.repository.PasswordRecoveryRepository
import dagger.Lazy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SupabasePasswordRecoveryRepository @Inject constructor(
    @Named("passwordRecovery") private val recoveryClient: Lazy<SupabaseClient>,
    private val primaryAuth: Lazy<Auth>
) : PasswordRecoveryRepository {
    override suspend fun recoverSession(credentials: PasswordRecoveryLink.Credentials): Result<Unit> = recoveryResult {
        val auth = recoveryClient.get().auth
        auth.awaitInitialization()
        auth.clearSession()
        val user = auth.retrieveUser(credentials.accessToken)
        auth.importSession(
            UserSession(
                accessToken = credentials.accessToken,
                refreshToken = credentials.refreshToken,
                expiresIn = credentials.expiresIn,
                tokenType = "bearer",
                user = user,
                type = "recovery"
            ),
            source = SessionSource.External
        )
    }

    override suspend fun updatePassword(password: String): Result<Unit> = recoveryResult {
        val auth = recoveryClient.get().auth
        check(auth.currentSessionOrNull() != null) { "Open a new password reset link to continue." }
        auth.updateUser { this.password = password }
    }

    override suspend fun clearRecoverySession(): Result<Unit> = recoveryResult {
        recoveryClient.get().auth.clearSession()
    }

    override suspend fun finishRecovery(): Result<Unit> = recoveryResult {
        clearRecoverySession().getOrThrow()
        // Return to login without persisting the temporary recovery session.
        primaryAuth.get().awaitInitialization()
        primaryAuth.get().clearSession()
    }

    private suspend fun recoveryResult(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
