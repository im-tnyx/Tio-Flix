package com.tioflix.app.ui.auth.login

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onSignupClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        state = state.value,
        onAction = { action ->
            when (action) {
                LoginAction.SignupClicked -> onSignupClick()
                LoginAction.ForgotPasswordClicked -> onForgotPasswordClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}
