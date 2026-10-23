package com.tioflix.app.ui.auth.reset

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tioflix.app.domain.model.PasswordRecoveryLink
import com.tioflix.app.domain.repository.PasswordRecoveryRepository
import com.tioflix.app.domain.usecase.UpdateRecoveredPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RecoveryStage { INACTIVE, VALIDATING, READY, UPDATING, UPDATED, FINISHING, INVALID }

data class ResetPasswordUiState(
    val stage: RecoveryStage = RecoveryStage.INACTIVE,
    val password: String = "",
    val confirmation: String = "",
    val errorMessage: String? = null
) {
    val isActive: Boolean get() = stage != RecoveryStage.INACTIVE
    val isBusy: Boolean get() = stage in setOf(RecoveryStage.VALIDATING, RecoveryStage.UPDATING, RecoveryStage.FINISHING)
    override fun toString(): String = "ResetPasswordUiState(stage=$stage)"
}

sealed interface ResetPasswordAction {
    data class PasswordChanged(val value: String) : ResetPasswordAction
    data class ConfirmationChanged(val value: String) : ResetPasswordAction
    data object Submit : ResetPasswordAction
    data object ContinueToLogin : ResetPasswordAction
    data object Cancel : ResetPasswordAction
}

data class ResetPasswordEffect(val loginMessage: String)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val repository: PasswordRecoveryRepository,
    private val updatePassword: UpdateRecoveredPasswordUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<ResetPasswordEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var recoveryJob: Job? = null

    fun handleDeepLink(url: String): Boolean {
        val link = PasswordRecoveryLink.parse(url)
        if (link == PasswordRecoveryLink.NotRecovery) return false
        if (_uiState.value.stage in setOf(RecoveryStage.UPDATING, RecoveryStage.UPDATED, RecoveryStage.FINISHING)) return true
        recoveryJob?.cancel()
        _uiState.value = ResetPasswordUiState(stage = RecoveryStage.VALIDATING)
        recoveryJob = viewModelScope.launch {
            if (link is PasswordRecoveryLink.Credentials) {
                repository.recoverSession(link)
                    .onSuccess { _uiState.value = ResetPasswordUiState(stage = RecoveryStage.READY) }
                    .onFailure { invalidLink() }
            } else {
                invalidLink()
            }
        }
        return true
    }

    private fun invalidLink() {
        _uiState.value = ResetPasswordUiState(
            stage = RecoveryStage.INVALID,
            errorMessage = "This reset link is invalid, expired, or could not be verified. Request a new link and try again."
        )
    }

    fun onAction(action: ResetPasswordAction) {
        val state = _uiState.value
        when (action) {
            is ResetPasswordAction.PasswordChanged -> if (state.stage == RecoveryStage.READY) {
                _uiState.update { it.copy(password = action.value, errorMessage = null) }
            }
            is ResetPasswordAction.ConfirmationChanged -> if (state.stage == RecoveryStage.READY) {
                _uiState.update { it.copy(confirmation = action.value, errorMessage = null) }
            }
            ResetPasswordAction.Submit -> submit()
            ResetPasswordAction.ContinueToLogin -> if (state.stage == RecoveryStage.UPDATED) finish()
            ResetPasswordAction.Cancel -> if (state.stage !in setOf(RecoveryStage.UPDATING, RecoveryStage.FINISHING, RecoveryStage.UPDATED)) cancel()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.stage != RecoveryStage.READY) return
        _uiState.update { it.copy(stage = RecoveryStage.UPDATING, errorMessage = null) }
        recoveryJob = viewModelScope.launch {
            updatePassword(state.password, state.confirmation)
                .onSuccess {
                    _uiState.value = ResetPasswordUiState(stage = RecoveryStage.UPDATED)
                    finish()
                }
                .onFailure {
                    _uiState.value = ResetPasswordUiState(
                        stage = RecoveryStage.READY,
                        errorMessage = if (it is IllegalArgumentException) it.message else "Unable to update your password. Try again or request a new reset link."
                    )
                }
        }
    }

    private fun finish() {
        _uiState.value = ResetPasswordUiState(stage = RecoveryStage.FINISHING)
        recoveryJob = viewModelScope.launch {
            repository.finishRecovery()
                .onSuccess { _effects.send(ResetPasswordEffect("Password updated. Sign in with your new password.")) }
                .onFailure {
                    _uiState.value = ResetPasswordUiState(
                        stage = RecoveryStage.UPDATED,
                        errorMessage = "Password updated. Tap Continue to sign in to finish clearing this session."
                    )
                }
        }
    }

    private fun cancel() {
        recoveryJob?.cancel()
        _uiState.value = ResetPasswordUiState(stage = RecoveryStage.FINISHING)
        recoveryJob = viewModelScope.launch {
            repository.clearRecoverySession()
                .onSuccess { _effects.send(ResetPasswordEffect("Request a new password reset email to try again.")) }
                .onFailure { invalidLink() }
        }
    }

    fun onLoginShown() {
        _uiState.value = ResetPasswordUiState()
    }
}

@Composable
fun ResetPasswordRoute(viewModel: ResetPasswordViewModel, onLogin: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect {
            onLogin(it.loginMessage)
            viewModel.onLoginShown()
        }
    }
    BackHandler {
        viewModel.onAction(if (state.stage == RecoveryStage.UPDATED) ResetPasswordAction.ContinueToLogin else ResetPasswordAction.Cancel)
    }
    ResetPasswordScreen(state, viewModel::onAction)
}

@Composable
fun ResetPasswordScreen(state: ResetPasswordUiState, onAction: (ResetPasswordAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Set a new password", style = MaterialTheme.typography.headlineMedium)
        if (state.isBusy) CircularProgressIndicator(Modifier.padding(top = 20.dp))
        if (state.stage == RecoveryStage.VALIDATING) Text("Verifying your reset link...", modifier = Modifier.padding(top = 12.dp))
        if (state.stage in setOf(RecoveryStage.READY, RecoveryStage.UPDATING)) {
            Text("Use at least 8 characters.", modifier = Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = { onAction(ResetPasswordAction.PasswordChanged(it)) },
                label = { Text("New password") },
                enabled = !state.isBusy,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            )
            OutlinedTextField(
                value = state.confirmation,
                onValueChange = { onAction(ResetPasswordAction.ConfirmationChanged(it)) },
                label = { Text("Confirm password") },
                enabled = !state.isBusy,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            Button(
                onClick = { onAction(ResetPasswordAction.Submit) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) { Text(if (state.stage == RecoveryStage.UPDATING) "Updating..." else "Update password") }
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        if (state.stage == RecoveryStage.UPDATED) {
            Button(onClick = { onAction(ResetPasswordAction.ContinueToLogin) }, modifier = Modifier.padding(top = 20.dp)) { Text("Continue to sign in") }
        } else {
            TextButton(onClick = { onAction(ResetPasswordAction.Cancel) }, enabled = state.stage !in setOf(RecoveryStage.UPDATING, RecoveryStage.FINISHING)) {
                Text("Back to sign in")
            }
        }
    }
}
