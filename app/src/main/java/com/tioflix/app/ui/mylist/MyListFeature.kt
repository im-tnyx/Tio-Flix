package com.tioflix.app.ui.mylist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.tioflix.app.domain.model.ContentItem
import com.tioflix.app.domain.model.ContentType
import com.tioflix.app.domain.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyListUiState(
    val isLoading: Boolean = true,
    val items: List<ContentItem> = emptyList(),
    val errorMessage: String? = null
)

sealed interface MyListAction {
    data object RetryClicked : MyListAction
    data class ContentClicked(val contentId: String) : MyListAction
}

sealed interface MyListEffect {
    data class NavigateContent(val contentId: String) : MyListEffect
}

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyListUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<MyListEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadFavorites()
    }

    fun onAction(action: MyListAction) {
        when (action) {
            MyListAction.RetryClicked -> loadFavorites()
            is MyListAction.ContentClicked -> viewModelScope.launch {
                _effects.send(MyListEffect.NavigateContent(action.contentId))
            }
        }
    }

    private fun loadFavorites() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        favoritesRepository.getFavorites()
            .onSuccess { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load your list."
                    )
                }
            }
    }
}

@Composable
fun MyListRoute(
    onContentClick: (String) -> Unit,
    viewModel: MyListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyListEffect.NavigateContent -> onContentClick(effect.contentId)
            }
        }
    }

    MyListScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun MyListScreen(
    state: MyListUiState,
    onAction: (MyListAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.errorMessage != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.errorMessage)
                Button(
                    onClick = { onAction(MyListAction.RetryClicked) },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Retry")
                }
            }

            state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your list is empty.")
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                        Text("My List", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Your saved movies and series.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                items(state.items, key = { it.id }) { item ->
                    MyListCard(
                        item = item,
                        onClick = { onAction(MyListAction.ContentClicked(item.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyListCard(
    item: ContentItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.posterUrl ?: item.backdropUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(92.dp)
                    .height(130.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(myListMeta(item), style = MaterialTheme.typography.bodyMedium)
                item.description?.let {
                    Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun myListMeta(item: ContentItem): String = buildList {
    add(if (item.type == ContentType.SERIES) "Series" else "Movie")
    item.releaseYear?.let { add(it.toString()) }
    item.maturityRating?.takeIf(String::isNotBlank)?.let(::add)
}.joinToString(" • ")
