package com.tioflix.app.domain.usecase

import com.tioflix.app.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(idToken: String, nonce: String): Result<Unit> {
        if (idToken.isBlank()) return Result.failure(IllegalArgumentException("Google ID token is missing."))
        if (nonce.isBlank()) return Result.failure(IllegalArgumentException("Google sign-in nonce is missing."))
        return repository.signInWithGoogleIdToken(idToken, nonce)
    }
}
