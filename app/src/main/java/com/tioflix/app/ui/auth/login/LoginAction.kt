package com.tioflix.app.ui.auth.login

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object SubmitEmailLogin : LoginAction
    data object ContinueWithGoogle : LoginAction
    data object SignupClicked : LoginAction
    data object ForgotPasswordClicked : LoginAction
}
