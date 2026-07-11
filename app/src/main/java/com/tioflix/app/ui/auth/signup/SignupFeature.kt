package com.tioflix.app.ui.auth.signup

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tioflix.app.domain.usecase.SignUpWithEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignupUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val message: String? = null
)

sealed interface SignupAction {
    data class EmailChanged(val value: String) : SignupAction
    data class PasswordChanged(val value: String) : SignupAction
    data object Submit : SignupAction
}

sealed interface SignupEffect { data object NavigateBack : SignupEffect }

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signUp: SignUpWithEmailUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<SignupEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: SignupAction) {
        when (action) {
            is SignupAction.EmailChanged -> _uiState.update { it.copy(email = action.value, message = null) }
            is SignupAction.PasswordChanged -> _uiState.update { it.copy(password = action.value, message = null) }
            SignupAction.Submit -> submit()
        }
    }

    private fun submit() = viewModelScope.launch {
        val state = _uiState.value
        if (state.isLoading) return@launch
        _uiState.update { it.copy(isLoading = true, message = null) }
        signUp(state.email, state.password)
            .onSuccess {
                _uiState.update { it.copy(message = "Account created. Check email confirmation if enabled.") }
                _effects.send(SignupEffect.NavigateBack)
            }
            .onFailure { error -> _uiState.update { it.copy(message = error.message ?: "Signup failed.") } }
        _uiState.update { it.copy(isLoading = false) }
    }
}

@Composable
fun SignupRoute(
    onCompleted: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { onCompleted() }
    }
    SignupScreen(state.value, viewModel::onAction)
}

@Composable
fun SignupScreen(state: SignupUiState, onAction: (SignupAction) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.email,
            onValueChange = { onAction(SignupAction.EmailChanged(it)) },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(SignupAction.PasswordChanged(it)) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        state.message?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
        Button(
            onClick = { onAction(SignupAction.Submit) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) { Text(if (state.isLoading) "Creating..." else "Create account") }
    }
}
