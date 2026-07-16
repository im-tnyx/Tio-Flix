package com.tioflix.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tioflix.app.domain.model.ContentCategory
import com.tioflix.app.domain.repository.CatalogRepository
import com.tioflix.app.domain.repository.FavoritesRepository
import com.tioflix.app.domain.repository.WatchHistoryRepository
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
    private val catalogRepository: CatalogRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val favoritesRepository: FavoritesRepository
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
            HomeAction.SearchClicked -> viewModelScope.launch {
                _effects.send(HomeEffect.NavigateSearch)
            }
            is HomeAction.ContentClicked -> viewModelScope.launch {
                _effects.send(HomeEffect.NavigateContentDetail(action.contentId))
            }
        }
    }

    private fun loadCatalog() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val catalogResult = catalogRepository.getHomeCatalog()
        val continueWatching = watchHistoryRepository.getContinueWatching().getOrDefault(emptyList())
        val favorites = favoritesRepository.getFavorites().getOrDefault(emptyList())

        catalogResult
            .onSuccess { catalog ->
                val categories = if (favorites.isNotEmpty()) {
                    listOf(
                        ContentCategory(
                            id = -1L,
                            slug = "my-list",
                            name = "My List",
                            sortOrder = -1,
                            items = favorites
                        )
                    ) + catalog.categories
                } else catalog.categories

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        catalog = catalog.copy(categories = categories),
                        continueWatching = continueWatching
                    )
                }
            }
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
