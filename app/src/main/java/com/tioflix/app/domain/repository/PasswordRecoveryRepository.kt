package com.tioflix.app.domain.repository

import com.tioflix.app.domain.model.PasswordRecoveryLink

interface PasswordRecoveryRepository {
    suspend fun recoverSession(credentials: PasswordRecoveryLink.Credentials): Result<Unit>
    suspend fun updatePassword(password: String): Result<Unit>
    suspend fun clearRecoverySession(): Result<Unit>
    suspend fun finishRecovery(): Result<Unit>
}
