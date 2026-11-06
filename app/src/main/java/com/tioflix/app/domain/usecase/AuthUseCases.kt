package com.tioflix.app.domain.usecase

import com.tioflix.app.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpWithEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email is required."))
        if (password.length < 8) return Result.failure(IllegalArgumentException("Password must be at least 8 characters."))
        return repository.signUpWithEmail(email.trim(), password)
    }
}

class ResendSignupConfirmationUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email is required."))
        return repository.resendSignupConfirmation(email.trim())
    }
}

class SendPasswordResetUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email is required."))
        return repository.sendPasswordReset(email.trim(), "tioflix://auth/reset-password")
    }
}

class HasSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Boolean = repository.hasSession()
}

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.signOut()
}
