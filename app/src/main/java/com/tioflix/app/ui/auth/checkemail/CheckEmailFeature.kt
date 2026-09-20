package com.tioflix.app.ui.auth.checkemail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tioflix.app.domain.usecase.HasSessionUseCase
import com.tioflix.app.domain.usecase.ResendSignupConfirmationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckEmailUiState(
    val isChecking: Boolean = true,
    val secondsElapsed: Int = 0,
    val isResending: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val message: String? = null
)

sealed interface CheckEmailEffect {
    data object NavigateHome : CheckEmailEffect
}

@HiltViewModel
class CheckEmailViewModel @Inject constructor(
    private val hasSession: HasSessionUseCase,
    private val resendSignupConfirmation: ResendSignupConfirmationUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckEmailUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<CheckEmailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        startVerificationPolling()
        startResendCooldownTicker()
    }

    private fun startVerificationPolling() {
        viewModelScope.launch {
            while (isActive) {
                if (hasSession()) {
                    _effects.send(CheckEmailEffect.NavigateHome)
                    return@launch
                }

                delay(2_000)
                _uiState.update { state ->
                    state.copy(secondsElapsed = state.secondsElapsed + 2)
                }
            }
        }
    }

    private fun startResendCooldownTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _uiState.update { state ->
                    if (state.resendCooldownSeconds > 0) {
                        state.copy(resendCooldownSeconds = state.resendCooldownSeconds - 1)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun resend(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(message = "Email missing hai. Signup dobara try karein.") }
            return
        }

        val state = _uiState.value
        if (state.isResending || state.resendCooldownSeconds > 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, message = null) }
            resendSignupConfirmation(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            resendCooldownSeconds = 60,
                            message = "Verification email resend kar diya gaya. Inbox, spam aur promotions check karein."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            message = error.message ?: "Verification email resend nahi ho paya."
                        )
                    }
                }
        }
    }
}

@Composable
fun CheckEmailRoute(
    email: String,
    onBackToLogin: (String) -> Unit,
    onVerified: () -> Unit,
    viewModel: CheckEmailViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CheckEmailEffect.NavigateHome -> onVerified()
            }
        }
    }

    CheckEmailScreen(
        email = email,
        state = state.value,
        onResend = { viewModel.resend(email) },
        onBackToLogin = { onBackToLogin(email) }
    )
}

@Composable
fun CheckEmailScreen(
    email: String,
    state: CheckEmailUiState,
    onResend: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bodyText = if (email.isBlank()) {
        "We sent a verification link to your email address. Open that email and verify your account. Keep this screen open and we will continue automatically."
    } else {
        "We sent a verification link to $email. Open that email and verify your account. Keep this screen open and we will continue automatically."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Check your email",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )

        Text(
            text = if (state.isChecking) {
                "Waiting for verification and checking automatically every few seconds."
            } else {
                "Verification check is paused."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Waiting for ${state.secondsElapsed}s",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "If you do not see it, check spam or promotions before trying again.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )

        state.message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedButton(
            onClick = onResend,
            enabled = !state.isResending && state.resendCooldownSeconds == 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            val resendLabel = when {
                state.isResending -> "Resending..."
                state.resendCooldownSeconds > 0 -> "Resend in ${state.resendCooldownSeconds}s"
                else -> "Resend email"
            }
            Text(resendLabel)
        }

        Button(
            onClick = onBackToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text("I verified, continue to sign in")
        }
    }
}
