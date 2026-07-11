package com.tioflix.app.ui.auth.login

sealed interface LoginEffect {
    data object NavigateHome : LoginEffect
}
