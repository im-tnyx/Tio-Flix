package com.tioflix.app.ui.auth.forgot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tioflix.app.domain.usecase.SendPasswordResetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val message: String? = null
)

sealed interface ForgotPasswordAction {
    data class EmailChanged(val value: String) : ForgotPasswordAction
    data object Submit : ForgotPasswordAction
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val sendReset: SendPasswordResetUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: ForgotPasswordAction) {
        when (action) {
            is ForgotPasswordAction.EmailChanged -> _uiState.update { it.copy(email = action.value, message = null) }
            ForgotPasswordAction.Submit -> submit()
        }
    }

    private fun submit() = viewModelScope.launch {
        val state = _uiState.value
        if (state.isLoading) return@launch
        _uiState.update { it.copy(isLoading = true, message = null) }
        sendReset(state.email)
            .onSuccess { _uiState.update { it.copy(message = "Password reset email sent.") } }
            .onFailure { error -> _uiState.update { it.copy(message = error.message ?: "Unable to send reset email.") } }
        _uiState.update { it.copy(isLoading = false) }
    }
}

@Composable
fun ForgotPasswordRoute(viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordScreen(state.value, viewModel::onAction)
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onAction: (ForgotPasswordAction) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Reset password", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.email,
            onValueChange = { onAction(ForgotPasswordAction.EmailChanged(it)) },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        )
        state.message?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
        Button(
            onClick = { onAction(ForgotPasswordAction.Submit) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) { Text(if (state.isLoading) "Sending..." else "Send reset email") }
    }
}
