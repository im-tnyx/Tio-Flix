package com.tioflix.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    onLoggedOut: () -> Unit,
    onContentClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.NavigateLogin -> onLoggedOut()
                is HomeEffect.NavigateContentDetail -> onContentClick(effect.contentId)
            }
        }
    }

    HomeScreen(
        state = state.value,
        onAction = viewModel::onAction
    )
}
