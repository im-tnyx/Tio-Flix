package com.tioflix.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tioflix.app.domain.repository.CatalogRepository
import com.tioflix.app.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val signOut: SignOutUseCase,
    private val catalogRepository: CatalogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { loadCatalog() }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.RetryClicked -> loadCatalog()
            HomeAction.LogoutClicked -> logout()
            is HomeAction.ContentClicked -> Unit
        }
    }

    private fun loadCatalog() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        catalogRepository.getHomeCatalog()
            .onSuccess { catalog -> _uiState.update { it.copy(isLoading = false, catalog = catalog) } }
            .onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Unable to load catalog.")
                }
            }
    }

    private fun logout() = viewModelScope.launch {
        signOut().onSuccess { _effects.send(HomeEffect.NavigateLogin) }
    }
}
