package com.tioflix.app.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tioflix.app.domain.usecase.SignInWithEmailUseCase
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
    private val signInWithEmail: SignInWithEmailUseCase
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
            LoginAction.ContinueWithGoogle -> _uiState.update {
                it.copy(errorMessage = "Google sign-in needs Credential Manager and Google client configuration.")
            }
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
                .onSuccess { _effects.send(LoginEffect.NavigateHome) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Unable to sign in.")
                    }
                }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
