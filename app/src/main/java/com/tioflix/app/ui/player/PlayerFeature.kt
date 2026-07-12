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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.tioflix.app.domain.repository.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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
    val errorMessage: String? = null
)

sealed interface PlayerAction {
    data object RetryClicked : PlayerAction
    data object BackClicked : PlayerAction
}

sealed interface PlayerEffect {
    data object NavigateBack : PlayerEffect
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {
    private val contentId: String = checkNotNull(savedStateHandle["contentId"])
    private val episodeId: String? = savedStateHandle.get<String>("episodeId")
        ?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { loadPlaybackSession() }

    fun onAction(action: PlayerAction) {
        when (action) {
            PlayerAction.RetryClicked -> loadPlaybackSession()
            PlayerAction.BackClicked -> viewModelScope.launch {
                _effects.send(PlayerEffect.NavigateBack)
            }
        }
    }

    private fun loadPlaybackSession() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, playbackUrl = null) }
        playbackRepository.createPlaybackSession(contentId, episodeId)
            .onSuccess { session ->
                _uiState.value = PlayerUiState(
                    isLoading = false,
                    title = session.title,
                    playbackUrl = session.playbackUrl,
                    startPositionMs = session.startPositionMs
                )
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to start playback."
                    )
                }
            }
    }
}

@Composable
fun PlayerRoute(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PlayerEffect.NavigateBack -> onBack()
            }
        }
    }

    PlayerScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.errorMessage != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp)
                )
                Button(onClick = { onAction(PlayerAction.RetryClicked) }) {
                    Text("Retry")
                }
                Button(
                    onClick = { onAction(PlayerAction.BackClicked) },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text("Back")
                }
            }
            state.playbackUrl != null -> Media3PlayerHost(
                playbackUrl = state.playbackUrl,
                startPositionMs = state.startPositionMs
            )
        }
    }
}

@Composable
private fun Media3PlayerHost(
    playbackUrl: String,
    startPositionMs: Long
) {
    val context = LocalContext.current
    val player = remember(playbackUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(playbackUrl))
            if (startPositionMs > 0L) seekTo(startPositionMs)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                controllerAutoShow = true
                controllerHideOnTouch = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                requestFocus()
            }
        },
        update = { it.player = player }
    )
}
