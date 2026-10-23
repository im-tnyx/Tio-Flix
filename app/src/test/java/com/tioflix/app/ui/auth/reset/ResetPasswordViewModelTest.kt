package com.tioflix.app.ui.auth.reset

import com.tioflix.app.domain.model.PasswordRecoveryLink
import com.tioflix.app.domain.repository.PasswordRecoveryRepository
import com.tioflix.app.domain.usecase.UpdateRecoveredPasswordUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResetPasswordViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeRecoveryRepository
    private lateinit var viewModel: ResetPasswordViewModel
    private val link = "tioflix://auth/reset-password#type=recovery&access_token=test-access&refresh_token=test-refresh&expires_in=3600&token_type=bearer"

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRecoveryRepository()
        viewModel = ResetPasswordViewModel(repository, UpdateRecoveredPasswordUseCase(repository))
    }

    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun ordinarySignupRemainsWithExistingAuthFlow() = runTest {
        assertFalse(viewModel.handleDeepLink("tioflix://auth#type=signup"))
        assertEquals(RecoveryStage.INACTIVE, viewModel.uiState.value.stage)
        assertEquals(0, repository.recoverCalls)
    }

    @Test fun formWaitsForServerValidation() = runTest {
        repository.validationGate = CompletableDeferred()
        assertTrue(viewModel.handleDeepLink(link))
        runCurrent()
        assertEquals(RecoveryStage.VALIDATING, viewModel.uiState.value.stage)
        viewModel.onAction(ResetPasswordAction.Submit)
        assertEquals(0, repository.updateCalls)
        repository.validationGate!!.complete(Unit)
        runCurrent()
        assertEquals(RecoveryStage.READY, viewModel.uiState.value.stage)
    }

    @Test fun invalidLinkCannotUpdatePassword() = runTest {
        viewModel.handleDeepLink("tioflix://auth/reset-password")
        runCurrent()
        viewModel.onAction(ResetPasswordAction.Submit)
        runCurrent()
        assertEquals(RecoveryStage.INVALID, viewModel.uiState.value.stage)
        assertEquals(0, repository.recoverCalls)
        assertEquals(0, repository.updateCalls)
    }

    @Test fun expiredSessionDoesNotEnableTheFormOrExposeServerErrors() = runTest {
        repository.recoverResult = Result.failure(IllegalStateException("sensitive response"))
        viewModel.handleDeepLink(link)
        runCurrent()
        assertEquals(RecoveryStage.INVALID, viewModel.uiState.value.stage)
        assertFalse(viewModel.uiState.value.errorMessage.orEmpty().contains("sensitive response"))
    }

    @Test fun shortOrMismatchedPasswordsNeverReachBackend() = runTest {
        ready()
        passwords("short", "short")
        viewModel.onAction(ResetPasswordAction.Submit)
        runCurrent()
        assertEquals(0, repository.updateCalls)
        assertTrue(viewModel.uiState.value.errorMessage.orEmpty().contains("8 characters"))
        passwords("test-password", "different-password")
        viewModel.onAction(ResetPasswordAction.Submit)
        runCurrent()
        assertEquals(0, repository.updateCalls)
        assertTrue(viewModel.uiState.value.errorMessage.orEmpty().contains("do not match"))
    }

    @Test fun successfulResetClearsSessionBeforeReturningToLogin() = runTest {
        ready()
        passwords(" test-password ", " test-password ")
        val effect = async { viewModel.effects.first() }
        viewModel.onAction(ResetPasswordAction.Submit)
        viewModel.onAction(ResetPasswordAction.Submit)
        runCurrent()
        assertEquals(listOf("recover", "update", "finish"), repository.operations)
        assertEquals(" test-password ", repository.submittedPassword)
        assertEquals(1, repository.updateCalls)
        assertTrue(effect.await().loginMessage.contains("Password updated"))
        assertEquals("", viewModel.uiState.value.password)
        assertEquals("", viewModel.uiState.value.confirmation)
        viewModel.onLoginShown()
        assertEquals(RecoveryStage.INACTIVE, viewModel.uiState.value.stage)
    }

    @Test fun cleanupRetryDoesNotChangePasswordAgain() = runTest {
        ready()
        repository.finishResult = Result.failure(IllegalStateException("local cleanup failed"))
        passwords("test-password", "test-password")
        viewModel.onAction(ResetPasswordAction.Submit)
        runCurrent()
        assertEquals(RecoveryStage.UPDATED, viewModel.uiState.value.stage)
        assertEquals("", viewModel.uiState.value.password)
        repository.finishResult = Result.success(Unit)
        val effect = async { viewModel.effects.first() }
        viewModel.onAction(ResetPasswordAction.ContinueToLogin)
        runCurrent()
        effect.await()
        assertEquals(1, repository.updateCalls)
        assertEquals(2, repository.finishCalls)
    }

    @Test fun serverUpdateFailureKeepsRecoveryScreenAndClearsPasswords() = runTest {
        ready()
        repository.updateResult = Result.failure(IllegalStateException("server detail"))
        passwords("test-password", "test-password")
        viewModel.onAction(ResetPasswordAction.Submit)
        runCurrent()
        assertEquals(RecoveryStage.READY, viewModel.uiState.value.stage)
        assertEquals(0, repository.finishCalls)
        assertEquals("", viewModel.uiState.value.password)
        assertFalse(viewModel.uiState.value.errorMessage.orEmpty().contains("server detail"))
    }

    @Test fun cancellingPendingValidationCannotReopenTheForm() = runTest {
        repository.validationGate = CompletableDeferred()
        viewModel.handleDeepLink(link)
        runCurrent()
        val effect = async { viewModel.effects.first() }
        viewModel.onAction(ResetPasswordAction.Cancel)
        runCurrent()
        effect.await()
        repository.validationGate!!.complete(Unit)
        runCurrent()
        assertEquals(1, repository.clearCalls)
        assertEquals(0, repository.finishCalls)
        viewModel.onLoginShown()
        assertEquals(RecoveryStage.INACTIVE, viewModel.uiState.value.stage)
    }

    @Test fun aNewWarmLinkClearsPreviousFormValues() = runTest {
        ready()
        passwords("test-password", "test-password")
        assertTrue(viewModel.handleDeepLink(link.replace("test-access", "another-access")))
        assertEquals("", viewModel.uiState.value.password)
        runCurrent()
        assertEquals(2, repository.recoverCalls)
        assertEquals(RecoveryStage.READY, viewModel.uiState.value.stage)
    }

    @Test fun linksAndBackAreIgnoredWhilePasswordUpdateIsInFlight() = runTest {
        ready()
        repository.updateGate = CompletableDeferred()
        passwords("test-password", "test-password")
        viewModel.onAction(ResetPasswordAction.Submit)
        runCurrent()
        assertTrue(viewModel.handleDeepLink(link))
        viewModel.onAction(ResetPasswordAction.Cancel)
        assertEquals(RecoveryStage.UPDATING, viewModel.uiState.value.stage)
        assertEquals(1, repository.recoverCalls)
        assertEquals(0, repository.clearCalls)
        repository.updateGate!!.complete(Unit)
        runCurrent()
    }

    private fun ready() {
        viewModel.handleDeepLink(link)
        dispatcher.scheduler.runCurrent()
    }

    private fun passwords(password: String, confirmation: String) {
        viewModel.onAction(ResetPasswordAction.PasswordChanged(password))
        viewModel.onAction(ResetPasswordAction.ConfirmationChanged(confirmation))
    }

    private class FakeRecoveryRepository : PasswordRecoveryRepository {
        var recoverCalls = 0
        var updateCalls = 0
        var finishCalls = 0
        var clearCalls = 0
        var submittedPassword: String? = null
        var recoverResult: Result<Unit> = Result.success(Unit)
        var updateResult: Result<Unit> = Result.success(Unit)
        var finishResult: Result<Unit> = Result.success(Unit)
        var validationGate: CompletableDeferred<Unit>? = null
        var updateGate: CompletableDeferred<Unit>? = null
        val operations = mutableListOf<String>()

        override suspend fun recoverSession(credentials: PasswordRecoveryLink.Credentials): Result<Unit> {
            recoverCalls++
            operations.add("recover")
            validationGate?.await()
            return recoverResult
        }

        override suspend fun updatePassword(password: String): Result<Unit> {
            updateCalls++
            submittedPassword = password
            operations.add("update")
            updateGate?.await()
            return updateResult
        }

        override suspend fun clearRecoverySession(): Result<Unit> {
            clearCalls++
            return Result.success(Unit)
        }

        override suspend fun finishRecovery(): Result<Unit> {
            finishCalls++
            operations.add("finish")
            return finishResult
        }
    }
}
