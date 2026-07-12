package com.tioflix.app.data.history

import com.tioflix.app.data.catalog.ContentDto
import com.tioflix.app.domain.model.ContentItem
import com.tioflix.app.domain.model.ContentType
import com.tioflix.app.domain.model.ContinueWatchingItem
import com.tioflix.app.domain.model.WatchProgress
import com.tioflix.app.domain.repository.WatchHistoryRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class WatchProgressDto(
    @SerialName("user_id") val userId: String,
    @SerialName("content_id") val contentId: String,
    @SerialName("episode_id") val episodeId: String? = null,
    @SerialName("position_ms") val positionMs: Long,
    @SerialName("duration_ms") val durationMs: Long,
    val completed: Boolean,
    @SerialName("last_watched_at") val lastWatchedAt: String? = null,
    val content: ContentDto? = null
)

@Singleton
class SupabaseWatchHistoryRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : WatchHistoryRepository {

    override suspend fun getProgress(contentId: String): Result<WatchProgress?> = runCatching {
        val userId = currentUserId()
        postgrest["watch_history"]
            .select {
                eq("user_id", userId)
                eq("content_id", contentId)
                limit(1)
            }
            .decodeList<WatchProgressDto>()
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun saveProgress(progress: WatchProgress): Result<Unit> = runCatching {
        val dto = WatchProgressDto(
            userId = currentUserId(),
            contentId = progress.contentId,
            episodeId = progress.episodeId,
            positionMs = progress.positionMs.coerceAtLeast(0L),
            durationMs = progress.durationMs.coerceAtLeast(0L),
            completed = progress.completed,
            lastWatchedAt = null
        )
        postgrest["watch_history"].insert(
            value = dto,
            upsert = true,
            onConflict = "user_id,content_id"
        )
    }

    override suspend fun getContinueWatching(limit: Int): Result<List<ContinueWatchingItem>> = runCatching {
        val userId = currentUserId()
        postgrest["watch_history"]
            .select(
                columns = Columns.raw(
                    """
                    user_id,
                    content_id,
                    episode_id,
                    position_ms,
                    duration_ms,
                    completed,
                    last_watched_at,
                    content (
                        id,
                        content_type,
                        title,
                        description,
                        poster_url,
                        backdrop_url,
                        release_year,
                        duration_minutes,
                        total_seasons,
                        maturity_rating,
                        language,
                        is_featured
                    )
                    """.trimIndent()
                )
            ) {
                eq("user_id", userId)
                eq("completed", false)
                gt("position_ms", 0)
                order("last_watched_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(limit.coerceIn(1, 50).toLong())
            }
            .decodeList<WatchProgressDto>()
            .mapNotNull { row ->
                val content = row.content ?: return@mapNotNull null
                ContinueWatchingItem(
                    content = content.toDomain(),
                    episodeId = row.episodeId,
                    positionMs = row.positionMs,
                    durationMs = row.durationMs,
                    progressFraction = if (row.durationMs > 0L) {
                        (row.positionMs.toFloat() / row.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                )
            }
    }

    private fun currentUserId(): String = auth.currentUserOrNull()?.id
        ?: error("No authenticated user is available.")

    private fun WatchProgressDto.toDomain() = WatchProgress(
        contentId = contentId,
        episodeId = episodeId,
        positionMs = positionMs,
        durationMs = durationMs,
        completed = completed
    )

    private fun ContentDto.toDomain() = ContentItem(
        id = id,
        type = ContentType.valueOf(contentType),
        title = title,
        description = description,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseYear = releaseYear,
        durationMinutes = durationMinutes,
        totalSeasons = totalSeasons,
        maturityRating = maturityRating,
        language = language,
        isFeatured = isFeatured
    )
}
