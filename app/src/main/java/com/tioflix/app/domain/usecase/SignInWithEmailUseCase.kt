package com.tioflix.app.domain.usecase

import com.tioflix.app.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email is required."))
        if (password.length < 8) return Result.failure(IllegalArgumentException("Password must be at least 8 characters."))
        return authRepository.signInWithEmail(email.trim(), password)
    }
}
