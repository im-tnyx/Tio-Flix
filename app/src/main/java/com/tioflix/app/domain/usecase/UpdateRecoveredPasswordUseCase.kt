package com.tioflix.app.domain.usecase

import com.tioflix.app.domain.repository.PasswordRecoveryRepository
import javax.inject.Inject

class UpdateRecoveredPasswordUseCase @Inject constructor(
    private val repository: PasswordRecoveryRepository
) {
    suspend operator fun invoke(password: String, confirmation: String): Result<Unit> {
        if (password.length < 8) return Result.failure(IllegalArgumentException("Password must be at least 8 characters."))
        if (password != confirmation) return Result.failure(IllegalArgumentException("Passwords do not match."))
        return repository.updatePassword(password)
    }
}
