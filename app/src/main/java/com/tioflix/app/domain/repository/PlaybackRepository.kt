package com.tioflix.app.domain.repository

import com.tioflix.app.domain.model.PlaybackSession

interface PlaybackRepository {
    suspend fun createPlaybackSession(
        contentId: String,
        episodeId: String?
    ): Result<PlaybackSession>
}
