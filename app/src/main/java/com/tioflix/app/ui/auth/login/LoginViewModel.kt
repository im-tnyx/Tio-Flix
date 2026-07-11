package com.tioflix.app.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tioflix.app.domain.usecase.SignInWithEmailUseCase
import com.tioflix.app.domain.usecase.SignInWithGoogleUseCase
import com.tioflix.app.domain.usecase.SyncCurrentProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithEmail: SignInWithEmailUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val syncCurrentProfile: SyncCurrentProfileUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> _uiState.update { it.copy(email = action.value, errorMessage = null) }
            is LoginAction.PasswordChanged -> _uiState.update { it.copy(password = action.value, errorMessage = null) }
            LoginAction.SubmitEmailLogin -> submitEmailLogin()
            is LoginAction.GoogleCredentialReceived -> submitGoogleLogin(action.idToken, action.nonce)
            is LoginAction.GoogleSignInFailed -> _uiState.update {
                it.copy(isLoading = false, errorMessage = action.message)
            }
            LoginAction.ContinueWithGoogle -> _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            LoginAction.SignupClicked,
            LoginAction.ForgotPasswordClicked -> Unit
        }
    }

    private fun submitEmailLogin() {
        val state = _uiState.value
        if (state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            signInWithEmail(state.email, state.password)
                .onSuccess { completeLogin() }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message ?: "Unable to sign in.") } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun submitGoogleLogin(idToken: String, nonce: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            signInWithGoogle(idToken, nonce)
                .onSuccess { completeLogin() }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message ?: "Google sign-in failed.") } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun completeLogin() {
        syncCurrentProfile()
            .onSuccess { _effects.send(LoginEffect.NavigateHome) }
            .onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Signed in, but profile sync failed.")
                }
            }
    }
}
