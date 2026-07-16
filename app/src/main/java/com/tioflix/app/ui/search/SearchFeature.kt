package com.tioflix.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.tioflix.app.domain.model.ContentItem
import com.tioflix.app.domain.model.ContentType
import com.tioflix.app.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<ContentItem> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    data object SubmitClicked : SearchAction
    data object RetryClicked : SearchAction
    data object BackClicked : SearchAction
    data class RecentClicked(val query: String) : SearchAction
    data class ResultClicked(val contentId: String) : SearchAction
}

sealed interface SearchEffect {
    data object NavigateBack : SearchEffect
    data class NavigateContent(val contentId: String) : SearchEffect
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<SearchEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var searchJob: Job? = null

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> {
                _uiState.update { it.copy(query = action.query, errorMessage = null) }
                scheduleSearch(action.query)
            }
            SearchAction.SubmitClicked, SearchAction.RetryClicked -> searchNow(_uiState.value.query)
            SearchAction.BackClicked -> viewModelScope.launch { _effects.send(SearchEffect.NavigateBack) }
            is SearchAction.RecentClicked -> {
                _uiState.update { it.copy(query = action.query) }
                searchNow(action.query)
            }
            is SearchAction.ResultClicked -> viewModelScope.launch {
                _effects.send(SearchEffect.NavigateContent(action.contentId))
            }
        }
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(isLoading = false, results = emptyList(), hasSearched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            performSearch(query)
        }
    }

    private fun searchNow(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch(query) }
    }

    private suspend fun performSearch(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.length < 2) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        catalogRepository.searchContent(query)
            .onSuccess { results ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        results = results,
                        hasSearched = true,
                        recentSearches = (listOf(query) + state.recentSearches)
                            .distinctBy(String::lowercase)
                            .take(8)
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = error.message ?: "Search failed."
                    )
                }
            }
    }
}

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onContentClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SearchEffect.NavigateBack -> onBack()
                is SearchEffect.NavigateContent -> onContentClick(effect.contentId)
            }
        }
    }
    SearchScreen(state, viewModel::onAction)
}

@Composable
fun SearchScreen(state: SearchUiState, onAction: (SearchAction) -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 720.dp
            val horizontalPadding = if (wide) 48.dp else 20.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = horizontalPadding,
                    vertical = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { onAction(SearchAction.BackClicked) }) { Text("Back") }
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = { onAction(SearchAction.QueryChanged(it)) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Search movies and series") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onAction(SearchAction.SubmitClicked) })
                        )
                        if (wide) Button(onClick = { onAction(SearchAction.SubmitClicked) }) { Text("Search") }
                    }
                }

                if (state.query.isBlank() && state.recentSearches.isNotEmpty()) {
                    item { Text("Recent searches", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    items(state.recentSearches) { query ->
                        OutlinedButton(onClick = { onAction(SearchAction.RecentClicked(query)) }) { Text(query) }
                    }
                }

                when {
                    state.isLoading -> item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.errorMessage != null -> item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.errorMessage)
                            Button(
                                onClick = { onAction(SearchAction.RetryClicked) },
                                modifier = Modifier.padding(top = 12.dp)
                            ) { Text("Retry") }
                        }
                    }
                    state.hasSearched && state.results.isEmpty() -> item {
                        Text("No movies or series found for “${state.query.trim()}”.")
                    }
                    state.results.isNotEmpty() -> {
                        item {
                            Text(
                                "${state.results.size} result${if (state.results.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(state.results, key = { it.id }) { item ->
                            SearchResultCard(item, wide) {
                                onAction(SearchAction.ResultClicked(item.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(item: ContentItem, wide: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).focusable(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.posterUrl ?: item.backdropUrl,
                contentDescription = item.title,
                modifier = Modifier.width(if (wide) 130.dp else 92.dp).height(if (wide) 180.dp else 130.dp),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(searchMeta(item), style = MaterialTheme.typography.bodyMedium)
                item.description?.let {
                    Text(it, maxLines = if (wide) 3 else 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (wide) Button(onClick = onClick) { Text("View") }
        }
    }
}

private fun searchMeta(item: ContentItem): String = buildList {
    add(if (item.type == ContentType.SERIES) "Series" else "Movie")
    item.releaseYear?.let { add(it.toString()) }
    item.language?.takeIf(String::isNotBlank)?.let(::add)
    item.maturityRating?.takeIf(String::isNotBlank)?.let(::add)
}.joinToString(" • ")
