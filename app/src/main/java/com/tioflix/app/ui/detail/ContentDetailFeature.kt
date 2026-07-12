package com.tioflix.app.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.tioflix.app.domain.model.ContentItem
import com.tioflix.app.domain.model.ContentType
import com.tioflix.app.domain.model.SeriesEpisode
import com.tioflix.app.domain.model.SeriesSeason
import com.tioflix.app.domain.repository.CatalogRepository
import com.tioflix.app.domain.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContentDetailUiState(
    val isLoading: Boolean = true,
    val content: ContentItem? = null,
    val seasons: List<SeriesSeason> = emptyList(),
    val selectedSeasonId: String? = null,
    val isFavorite: Boolean = false,
    val isFavoriteUpdating: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedSeason: SeriesSeason?
        get() = seasons.firstOrNull { it.id == selectedSeasonId } ?: seasons.firstOrNull()
}

sealed interface ContentDetailAction {
    data object RetryClicked : ContentDetailAction
    data object BackClicked : ContentDetailAction
    data object PlayClicked : ContentDetailAction
    data object FavoriteClicked : ContentDetailAction
    data class SeasonClicked(val seasonId: String) : ContentDetailAction
    data class EpisodeClicked(val episodeId: String) : ContentDetailAction
}

sealed interface ContentDetailEffect {
    data object NavigateBack : ContentDetailEffect
    data class OpenPlayer(val contentId: String, val episodeId: String? = null) : ContentDetailEffect
}

@HiltViewModel
class ContentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {
    private val contentId: String = checkNotNull(savedStateHandle["contentId"])
    private val _uiState = MutableStateFlow(ContentDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<ContentDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { load() }

    fun onAction(action: ContentDetailAction) {
        when (action) {
            ContentDetailAction.RetryClicked -> load()
            ContentDetailAction.BackClicked -> viewModelScope.launch { _effects.send(ContentDetailEffect.NavigateBack) }
            ContentDetailAction.PlayClicked -> openDefaultPlayer()
            ContentDetailAction.FavoriteClicked -> toggleFavorite()
            is ContentDetailAction.SeasonClicked -> _uiState.update { it.copy(selectedSeasonId = action.seasonId) }
            is ContentDetailAction.EpisodeClicked -> viewModelScope.launch {
                _effects.send(ContentDetailEffect.OpenPlayer(contentId, action.episodeId))
            }
        }
    }

    private fun load() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val favorite = favoritesRepository.isFavorite(contentId).getOrDefault(false)
        catalogRepository.getContent(contentId)
            .onSuccess { content ->
                if (content.type == ContentType.SERIES) {
                    catalogRepository.getSeriesSeasons(contentId)
                        .onSuccess { seasons ->
                            _uiState.value = ContentDetailUiState(
                                isLoading = false,
                                content = content,
                                seasons = seasons,
                                selectedSeasonId = seasons.firstOrNull()?.id,
                                isFavorite = favorite
                            )
                        }
                        .onFailure(::setError)
                } else {
                    _uiState.value = ContentDetailUiState(
                        isLoading = false,
                        content = content,
                        isFavorite = favorite
                    )
                }
            }
            .onFailure(::setError)
    }

    private fun toggleFavorite() {
        val previous = _uiState.value
        if (previous.isFavoriteUpdating) return
        val target = !previous.isFavorite
        _uiState.update { it.copy(isFavorite = target, isFavoriteUpdating = true) }
        viewModelScope.launch {
            favoritesRepository.setFavorite(contentId, target)
                .onSuccess { _uiState.update { it.copy(isFavoriteUpdating = false) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isFavorite = previous.isFavorite,
                            isFavoriteUpdating = false,
                            errorMessage = error.message ?: "Unable to update My List."
                        )
                    }
                }
        }
    }

    private fun setError(error: Throwable) {
        _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to load content details.") }
    }

    private fun openDefaultPlayer() = viewModelScope.launch {
        val state = _uiState.value
        val episodeId = if (state.content?.type == ContentType.SERIES) {
            state.selectedSeason?.episodes?.firstOrNull()?.id
        } else null
        _effects.send(ContentDetailEffect.OpenPlayer(contentId, episodeId))
    }
}

@Composable
fun ContentDetailRoute(
    onBack: () -> Unit,
    onOpenPlayer: (contentId: String, episodeId: String?) -> Unit,
    viewModel: ContentDetailViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ContentDetailEffect.NavigateBack -> onBack()
                is ContentDetailEffect.OpenPlayer -> onOpenPlayer(effect.contentId, effect.episodeId)
            }
        }
    }
    ContentDetailScreen(state.value, viewModel::onAction)
}

@Composable
fun ContentDetailScreen(state: ContentDetailUiState, onAction: (ContentDetailAction) -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.content == null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.errorMessage ?: "Unable to load content details.")
                Button(onClick = { onAction(ContentDetailAction.RetryClicked) }, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
            }
            else -> DetailContent(state, onAction)
        }
    }
}

@Composable
private fun DetailContent(state: ContentDetailUiState, onAction: (ContentDetailAction) -> Unit) {
    val content = requireNotNull(state.content)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp
        val pagePadding = if (isWide) 48.dp else 20.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = pagePadding, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { OutlinedButton(onClick = { onAction(ContentDetailAction.BackClicked) }) { Text("Back") } }
            item {
                if (isWide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        DetailPoster(content, Modifier.width(260.dp))
                        DetailInfo(content, state, onAction, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        DetailPoster(content, Modifier.fillMaxWidth().height(260.dp))
                        DetailInfo(content, state, onAction, Modifier.fillMaxWidth())
                    }
                }
            }
            if (content.type == ContentType.SERIES) {
                item { Text("Seasons", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.seasons.forEach { season ->
                            if (state.selectedSeason?.id == season.id) {
                                Button(onClick = { onAction(ContentDetailAction.SeasonClicked(season.id)) }) { Text("Season ${season.seasonNumber}") }
                            } else {
                                OutlinedButton(onClick = { onAction(ContentDetailAction.SeasonClicked(season.id)) }) { Text("Season ${season.seasonNumber}") }
                            }
                        }
                    }
                }
                val episodes = state.selectedSeason?.episodes.orEmpty()
                if (episodes.isEmpty()) item { Text("No published episodes available.") }
                else items(episodes, key = { it.id }) { episode ->
                    EpisodeCard(episode) { onAction(ContentDetailAction.EpisodeClicked(episode.id)) }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun DetailPoster(content: ContentItem, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        AsyncImage(
            model = content.backdropUrl ?: content.posterUrl,
            contentDescription = content.title,
            modifier = Modifier.fillMaxWidth().height(360.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun DetailInfo(
    content: ContentItem,
    state: ContentDetailUiState,
    onAction: (ContentDetailAction) -> Unit,
    modifier: Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(content.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(detailMeta(content), style = MaterialTheme.typography.titleMedium)
        content.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onAction(ContentDetailAction.PlayClicked) }, modifier = Modifier.focusable()) {
                Text(if (content.type == ContentType.SERIES) "Play first episode" else "Play")
            }
            OutlinedButton(
                onClick = { onAction(ContentDetailAction.FavoriteClicked) },
                enabled = !state.isFavoriteUpdating,
                modifier = Modifier.focusable()
            ) {
                Text(if (state.isFavorite) "Remove from My List" else "Add to My List")
            }
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EpisodeCard(episode: SeriesEpisode, onClick: () -> Unit) {
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
                model = episode.thumbnailUrl,
                contentDescription = episode.title,
                modifier = Modifier.width(150.dp).height(84.dp),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f)) {
                Text("E${episode.episodeNumber} • ${episode.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                episode.description?.let { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                Text("${episode.durationMinutes} min", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onClick) { Text("Play") }
        }
    }
}

private fun detailMeta(content: ContentItem): String = buildList {
    add(if (content.type == ContentType.SERIES) "Series" else "Movie")
    content.releaseYear?.let { add(it.toString()) }
    content.maturityRating?.let(::add)
    content.language?.let(::add)
    if (content.type == ContentType.MOVIE) content.durationMinutes?.let { add("$it min") }
    else content.totalSeasons?.let { add("$it season${if (it == 1) "" else "s"}") }
}.joinToString(" • ")
