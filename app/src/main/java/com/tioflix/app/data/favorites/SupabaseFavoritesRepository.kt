package com.tioflix.app.data.favorites

import com.tioflix.app.data.catalog.ContentDto
import com.tioflix.app.domain.model.ContentItem
import com.tioflix.app.domain.model.ContentType
import com.tioflix.app.domain.repository.FavoritesRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class FavoriteInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("content_id") val contentId: String
)

@Serializable
private data class FavoriteRowDto(
    @SerialName("content_id") val contentId: String,
    val content: ContentDto? = null
)

@Singleton
class SupabaseFavoritesRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : FavoritesRepository {

    override suspend fun isFavorite(contentId: String): Result<Boolean> = runCatching {
        postgrest["favorites"]
            .select {
                eq("user_id", currentUserId())
                eq("content_id", contentId)
                limit(1)
            }
            .decodeList<FavoriteRowDto>()
            .isNotEmpty()
    }

    override suspend fun setFavorite(contentId: String, isFavorite: Boolean): Result<Unit> = runCatching {
        val userId = currentUserId()
        if (isFavorite) {
            postgrest["favorites"].insert(
                value = FavoriteInsertDto(userId, contentId),
                upsert = true,
                onConflict = "user_id,content_id"
            )
        } else {
            postgrest["favorites"].delete {
                eq("user_id", userId)
                eq("content_id", contentId)
            }
        }
    }

    override suspend fun getFavorites(limit: Int): Result<List<ContentItem>> = runCatching {
        postgrest["favorites"]
            .select(
                columns = Columns.raw(
                    """
                    content_id,
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
                eq("user_id", currentUserId())
                order("created_at", Order.DESCENDING)
                limit(limit.coerceIn(1, 50).toLong())
            }
            .decodeList<FavoriteRowDto>()
            .mapNotNull { it.content?.toDomain() }
    }

    private fun currentUserId(): String = auth.currentUserOrNull()?.id
        ?: error("No authenticated user is available.")

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
