package com.tioflix.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tioflix.app.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val displayName: String = "Profile",
    val email: String = "Not available",
    val provider: String = "email"
)

sealed interface SettingsAction {
    data object LogoutClicked : SettingsAction
}

sealed interface SettingsEffect {
    data object NavigateLogin : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: Auth,
    private val signOut: SignOutUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadProfile()
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.LogoutClicked -> logout()
        }
    }

    private fun loadProfile() {
        val user = auth.currentUserOrNull()
        val displayName = user?.userMetadata?.get("full_name")?.toString()?.trim('"')
            ?: user?.userMetadata?.get("name")?.toString()?.trim('"')
            ?: "Profile"
        val provider = user?.appMetadata
            ?.get("provider")
            ?.toString()
            ?.trim('"')
            ?: user?.identities?.firstOrNull()?.provider
            ?: "email"

        _uiState.update {
            it.copy(
                displayName = displayName,
                email = user?.email ?: "Not available",
                provider = provider
            )
        }
    }

    private fun logout() = viewModelScope.launch {
        signOut().onSuccess {
            _effects.send(SettingsEffect.NavigateLogin)
        }
    }
}

@Composable
fun SettingsRoute(
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateLogin -> onLoggedOut()
            }
        }
    }

    SettingsScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Account and app preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(state.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(state.email, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Signed in with ${state.provider}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Playback and parental controls will live here next.", style = MaterialTheme.typography.bodyLarge)
                    Text("For now, this tab gives users a clear account entry point instead of hiding everything behind Home.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Button(
                onClick = { onAction(SettingsAction.LogoutClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Logout")
            }
        }
    }
}
