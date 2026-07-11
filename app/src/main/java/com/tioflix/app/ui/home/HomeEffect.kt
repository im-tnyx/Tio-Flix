package com.tioflix.app.ui.home

sealed interface HomeEffect {
    data object NavigateLogin : HomeEffect
}
