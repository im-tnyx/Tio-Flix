package com.tioflix.app.ui.home

sealed interface HomeEffect {
    data class NavigateContentDetail(val contentId: String) : HomeEffect
}
