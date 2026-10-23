package com.tioflix.app.ui.auth.reset

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tioflix.app.core.design.TioFlixTheme
import com.tioflix.app.domain.model.PasswordRecoveryLink
import com.tioflix.app.domain.repository.PasswordRecoveryRepository
import com.tioflix.app.domain.usecase.UpdateRecoveredPasswordUseCase
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResetPasswordRouteTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val link = "tioflix://auth/reset-password#type=recovery&access_token=test-access&refresh_token=test-refresh&expires_in=3600&token_type=bearer"
    private val repository = FakeRecoveryRepository()
    private val viewModel = ResetPasswordViewModel(repository, UpdateRecoveredPasswordUseCase(repository))

    @Test fun formAppearsOnlyAfterRecoveryValidation() {
        repository.validationGate = CompletableDeferred()
        showRoute()
        composeRule.runOnIdle { assertTrue(viewModel.handleDeepLink(link)) }

        composeRule.onNodeWithText("Verifying your reset link...").assertExists()
        composeRule.onNodeWithText("New password").assertDoesNotExist()

        composeRule.runOnIdle { repository.validationGate!!.complete(Unit) }
        composeRule.onNodeWithText("New password").assertExists()
        composeRule.onNodeWithText("Confirm password").assertExists()
    }

    @Test fun formValidationPreventsBackendUpdate() {
        showReadyRoute()
        enterPasswords("short", "short")
        submit()
        composeRule.onNodeWithText("Password must be at least 8 characters.").assertExists()

        composeRule.onNode(hasSetTextAction() and hasText("New password")).performTextReplacement("test-password")
        composeRule.onNode(hasSetTextAction() and hasText("Confirm password")).performTextReplacement("other-password")
        submit()
        composeRule.onNodeWithText("Passwords do not match.").assertExists()
        composeRule.runOnIdle { assertEquals(0, repository.updateCalls) }
    }

    @Test fun successfulUpdateClearsSessionAndReturnsToLogin() {
        showReadyRoute()
        enterPasswords("test-password", "test-password")
        submit()

        composeRule.onNodeWithText("Login destination: Password updated. Sign in with your new password.").assertExists()
        composeRule.runOnIdle {
            assertEquals(listOf("recover", "update", "finish"), repository.operations)
            assertEquals("test-password", repository.submittedPassword)
            assertEquals(1, repository.updateCalls)
            assertEquals(RecoveryStage.INACTIVE, viewModel.uiState.value.stage)
        }
    }

    @Test fun cleanupRetryDoesNotSubmitPasswordAgain() {
        repository.finishResult = Result.failure(IllegalStateException("local cleanup failed"))
        showReadyRoute()
        enterPasswords("test-password", "test-password")
        submit()

        composeRule.onNodeWithText("Password updated. Tap Continue to sign in to finish clearing this session.").assertExists()
        composeRule.onNodeWithText("New password").assertDoesNotExist()
        composeRule.runOnIdle { repository.finishResult = Result.success(Unit) }
        composeRule.onNodeWithText("Continue to sign in").performScrollTo().performClick()

        composeRule.onNodeWithText("Login destination: Password updated. Sign in with your new password.").assertExists()
        composeRule.runOnIdle {
            assertEquals(1, repository.updateCalls)
            assertEquals(2, repository.finishCalls)
        }
    }

    @Test fun invalidLinkCannotOpenFormAndBackClearsRecovery() {
        showRoute()
        composeRule.runOnIdle { assertTrue(viewModel.handleDeepLink("tioflix://auth/reset-password")) }
        composeRule.onNodeWithText("This reset link is invalid, expired, or could not be verified. Request a new link and try again.").assertExists()
        composeRule.onNodeWithText("New password").assertDoesNotExist()
        composeRule.onNodeWithText("Back to sign in").performClick()

        composeRule.onNodeWithText("Login destination: Request a new password reset email to try again.").assertExists()
        composeRule.runOnIdle {
            assertEquals(0, repository.updateCalls)
            assertEquals(1, repository.clearCalls)
        }
    }

    @Test fun systemBackCancelsPendingValidationWithoutOpeningForm() {
        repository.validationGate = CompletableDeferred()
        showRoute()
        composeRule.runOnIdle { assertTrue(viewModel.handleDeepLink(link)) }
        composeRule.onNodeWithText("Verifying your reset link...").assertExists()

        Espresso.pressBack()
        composeRule.onNodeWithText("Login destination: Request a new password reset email to try again.").assertExists()
        composeRule.runOnIdle { repository.validationGate!!.complete(Unit) }
        composeRule.onNodeWithText("New password").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, repository.clearCalls)
            assertEquals(0, repository.updateCalls)
        }
    }

    private fun showRoute() {
        composeRule.setContent {
            TioFlixTheme {
                var loginMessage by remember { mutableStateOf<String?>(null) }
                if (loginMessage == null) {
                    ResetPasswordRoute(viewModel) { loginMessage = it }
                } else {
                    Text("Login destination: $loginMessage")
                }
            }
        }
    }

    private fun showReadyRoute() {
        showRoute()
        composeRule.runOnIdle { assertTrue(viewModel.handleDeepLink(link)) }
        composeRule.onNodeWithText("New password").assertExists()
    }

    private fun enterPasswords(password: String, confirmation: String) {
        composeRule.onNode(hasSetTextAction() and hasText("New password")).performTextInput(password)
        composeRule.onNode(hasSetTextAction() and hasText("Confirm password")).performTextInput(confirmation)
    }

    private fun submit() {
        composeRule.onNodeWithText("Update password").performScrollTo().performClick()
    }

    private class FakeRecoveryRepository : PasswordRecoveryRepository {
        var validationGate: CompletableDeferred<Unit>? = null
        var finishResult: Result<Unit> = Result.success(Unit)
        var updateCalls = 0
        var finishCalls = 0
        var clearCalls = 0
        var submittedPassword: String? = null
        val operations = mutableListOf<String>()

        override suspend fun recoverSession(credentials: PasswordRecoveryLink.Credentials): Result<Unit> {
            operations.add("recover")
            validationGate?.await()
            return Result.success(Unit)
        }

        override suspend fun updatePassword(password: String): Result<Unit> {
            operations.add("update")
            updateCalls++
            submittedPassword = password
            return Result.success(Unit)
        }

        override suspend fun clearRecoverySession(): Result<Unit> {
            clearCalls++
            return Result.success(Unit)
        }

        override suspend fun finishRecovery(): Result<Unit> {
            operations.add("finish")
            finishCalls++
            return finishResult
        }
    }
}
