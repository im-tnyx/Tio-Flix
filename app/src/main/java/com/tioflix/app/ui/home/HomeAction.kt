package com.tioflix.app.ui.home

sealed interface HomeAction {
    data object RetryClicked : HomeAction
}
