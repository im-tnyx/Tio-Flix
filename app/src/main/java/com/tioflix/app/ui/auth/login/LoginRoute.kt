package com.tioflix.app.ui.auth.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tioflix.app.data.auth.GoogleCredentialProvider
import kotlinx.coroutines.launch

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onSignupClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleCredentialProvider = remember { GoogleCredentialProvider() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateHome -> onLoginSuccess()
            }
        }
    }

    LoginScreen(
        state = state.value,
        onAction = { action ->
            when (action) {
                LoginAction.SignupClicked -> onSignupClick()
                LoginAction.ForgotPasswordClicked -> onForgotPasswordClick()
                LoginAction.ContinueWithGoogle -> {
                    viewModel.onAction(action)
                    scope.launch {
                        runCatching { googleCredentialProvider.getCredential(context) }
                            .onSuccess { result ->
                                viewModel.onAction(
                                    LoginAction.GoogleCredentialReceived(
                                        idToken = result.idToken,
                                        nonce = result.rawNonce
                                    )
                                )
                            }
                            .onFailure { error ->
                                val message = if (error is GetCredentialCancellationException) {
                                    "Google sign-in was cancelled."
                                } else {
                                    error.message ?: "Unable to open Google sign-in."
                                }
                                viewModel.onAction(LoginAction.GoogleSignInFailed(message))
                            }
                    }
                }
                else -> viewModel.onAction(action)
            }
        }
    )
}
