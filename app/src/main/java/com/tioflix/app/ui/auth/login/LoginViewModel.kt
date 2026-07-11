package com.tioflix.app.ui.auth.login

import androidx.lifecycle.ViewModel
import com.tioflix.app.core.config.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> _uiState.update { it.copy(email = action.value, errorMessage = null) }
            is LoginAction.PasswordChanged -> _uiState.update { it.copy(password = action.value, errorMessage = null) }
            LoginAction.SubmitEmailLogin,
            LoginAction.ContinueWithGoogle -> validateConfiguration()
            LoginAction.SignupClicked,
            LoginAction.ForgotPasswordClicked -> Unit
        }
    }

    private fun validateConfiguration() {
        if (!AppConfig.isSupabaseConfigured) {
            _uiState.update {
                it.copy(errorMessage = "Supabase is not configured yet. Add URL and anon key through secure build configuration.")
            }
        }
    }
}
