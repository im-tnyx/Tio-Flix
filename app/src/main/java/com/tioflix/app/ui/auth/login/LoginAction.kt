package com.tioflix.app.ui.auth.login

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object SubmitEmailLogin : LoginAction
    data object ContinueWithGoogle : LoginAction
    data class GoogleCredentialReceived(
        val idToken: String,
        val nonce: String
    ) : LoginAction
    data class GoogleSignInFailed(val message: String) : LoginAction
    data object SignupClicked : LoginAction
    data object ForgotPasswordClicked : LoginAction
}
