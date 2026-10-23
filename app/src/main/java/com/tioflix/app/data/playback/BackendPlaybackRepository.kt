package com.tioflix.app.data.playback

import com.tioflix.app.core.config.AppConfig
import com.tioflix.app.domain.model.PlaybackSession
import com.tioflix.app.domain.repository.PlaybackRepository
import io.github.jan.supabase.auth.Auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class PlaybackSessionRequest(
    @SerialName("content_id") val contentId: String,
    @SerialName("episode_id") val episodeId: String? = null,
    val platform: String = "android"
)

@Serializable
private data class PlaybackSessionResponse(
    @SerialName("playback_url") val playbackUrl: String,
    val title: String,
    @SerialName("start_position_ms") val startPositionMs: Long = 0L,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Singleton
class BackendPlaybackRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val auth: Auth
) : PlaybackRepository {

    override suspend fun createPlaybackSession(
        contentId: String,
        episodeId: String?
    ): Result<PlaybackSession> = runCatching {
        check(AppConfig.isPlaybackApiConfigured) {
            "Playback API is not configured. Add an HTTPS PLAYBACK_API_BASE_URL in local.properties."
        }

        val accessToken = auth.currentSessionOrNull()?.accessToken
            ?: error("Your session has expired. Sign in again.")

        val response = httpClient.post(
            "${AppConfig.playbackApiBaseUrl}/playback-session"
        ) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(PlaybackSessionRequest(contentId, episodeId))
        }.body<PlaybackSessionResponse>()

        check(response.playbackUrl.startsWith("https://")) {
            "Playback service returned an invalid media URL."
        }

        PlaybackSession(
            playbackUrl = response.playbackUrl,
            title = response.title,
            startPositionMs = response.startPositionMs.coerceAtLeast(0L),
            expiresAt = response.expiresAt
        )
    }
}
