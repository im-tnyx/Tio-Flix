package com.tioflix.app.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.tioflix.app.domain.model.WatchProgress
import com.tioflix.app.domain.repository.PlaybackRepository
import com.tioflix.app.domain.repository.WatchHistoryRepository
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

data class PlayerUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val playbackUrl: String? = null,
    val startPositionMs: Long = 0L,
    val nextEpisodePrompt: NextEpisodePrompt? = null,
    val seriesCompleted: Boolean = false,
    val errorMessage: String? = null
)

sealed interface PlayerAction {
    data object RetryClicked : PlayerAction
    data object BackClicked : PlayerAction
    data object PlaybackEnded : PlayerAction
    data object PlayNextClicked : PlayerAction
    data object CancelAutoplayClicked : PlayerAction
    data class ProgressChanged(val positionMs: Long, val durationMs: Long) : PlayerAction
}

sealed interface PlayerEffect { data object NavigateBack : PlayerEffect }

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playbackRepository: PlaybackRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val nextEpisodeResolver: NextEpisodeResolver
) : ViewModel() {
    private val contentId: String = checkNotNull(savedStateHandle["contentId"])
    private var episodeId: String? = savedStateHandle.get<String>("episodeId")?.takeIf(String::isNotBlank)
    private var countdownJob: Job? = null
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { load() }

    fun onAction(action: PlayerAction) {
        when (action) {
            PlayerAction.RetryClicked -> load()
            PlayerAction.BackClicked -> viewModelScope.launch { _effects.send(PlayerEffect.NavigateBack) }
            PlayerAction.PlaybackEnded -> resolveNextEpisode()
            PlayerAction.PlayNextClicked -> playNext()
            PlayerAction.CancelAutoplayClicked -> cancelAutoplay()
            is PlayerAction.ProgressChanged -> saveProgress(action.positionMs, action.durationMs)
        }
    }

    private fun load() = viewModelScope.launch {
        countdownJob?.cancel()
        _uiState.update { PlayerUiState(isLoading = true) }
        playbackRepository.createPlaybackSession(contentId, episodeId)
            .onSuccess { session ->
                val progress = watchHistoryRepository.getProgress(contentId).getOrNull()
                val resume = if (progress?.completed == false && progress.episodeId == episodeId) {
                    maxOf(session.startPositionMs, progress.positionMs)
                } else session.startPositionMs
                _uiState.value = PlayerUiState(false, session.title, session.playbackUrl, resume)
            }
            .onFailure { error ->
                _uiState.value = PlayerUiState(isLoading = false, errorMessage = error.message ?: "Unable to start playback.")
            }
    }

    private fun resolveNextEpisode() {
        val current = episodeId ?: return
        viewModelScope.launch {
            val next = nextEpisodeResolver(contentId, current)
            if (next == null) {
                _uiState.update { it.copy(seriesCompleted = true) }
            } else {
                _uiState.update { it.copy(nextEpisodePrompt = NextEpisodePrompt(next)) }
                countdownJob = launch {
                    for (seconds in 9 downTo 0) {
                        delay(1_000)
                        _uiState.update { state ->
                            state.copy(nextEpisodePrompt = state.nextEpisodePrompt?.copy(secondsRemaining = seconds))
                        }
                    }
                    playNext()
                }
            }
        }
    }

    private fun playNext() {
        val next = _uiState.value.nextEpisodePrompt?.episode ?: return
        countdownJob?.cancel()
        episodeId = next.id
        load()
    }

    private fun cancelAutoplay() {
        countdownJob?.cancel()
        _uiState.update { it.copy(nextEpisodePrompt = null) }
    }

    private fun saveProgress(positionMs: Long, durationMs: Long) {
        if (positionMs < 0 || durationMs <= 0) return
        val completed = positionMs >= durationMs * 0.9
        viewModelScope.launch {
            watchHistoryRepository.saveProgress(
                WatchProgress(contentId, episodeId, if (completed) durationMs else positionMs, durationMs, completed)
            )
        }
    }
}

@Composable
fun PlayerRoute(onBack: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { if (it == PlayerEffect.NavigateBack) onBack() } }
    PlayerScreen(state, viewModel::onAction)
}

@Composable
fun PlayerScreen(state: PlayerUiState, onAction: (PlayerAction) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.errorMessage != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.errorMessage, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(24.dp))
                Button(onClick = { onAction(PlayerAction.RetryClicked) }) { Text("Retry") }
                Button(onClick = { onAction(PlayerAction.BackClicked) }, modifier = Modifier.padding(top = 12.dp)) { Text("Back") }
            }
            state.playbackUrl != null -> Media3PlayerHost(
                state.playbackUrl,
                state.startPositionMs,
                { position, duration -> onAction(PlayerAction.ProgressChanged(position, duration)) },
                { onAction(PlayerAction.PlaybackEnded) }
            )
        }
        state.nextEpisodePrompt?.let {
            NextEpisodeOverlay(
                prompt = it,
                onPlayNext = { onAction(PlayerAction.PlayNextClicked) },
                onCancel = { onAction(PlayerAction.CancelAutoplayClicked) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
            )
        }
        if (state.seriesCompleted) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)) {
                Text("You finished this series.", modifier = Modifier.padding(20.dp))
            }
        }
    }
}

@Composable
private fun Media3PlayerHost(
    playbackUrl: String,
    startPositionMs: Long,
    onProgress: (Long, Long) -> Unit,
    onEnded: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(playbackUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(playbackUrl))
            if (startPositionMs > 0) seekTo(startPositionMs)
            prepare()
            playWhenReady = true
        }
    }
    LaunchedEffect(player) {
        while (true) {
            delay(15_000)
            onProgress(player.currentPosition, player.duration)
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onEnded()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            onProgress(player.currentPosition, player.duration)
            player.release()
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                controllerAutoShow = true
                controllerHideOnTouch = true
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                requestFocus()
            }
        },
        update = { it.player = player }
    )
}
