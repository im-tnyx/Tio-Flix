package com.tioflix.app.data.catalog

import com.tioflix.app.domain.model.ContentCategory
import com.tioflix.app.domain.model.ContentItem
import com.tioflix.app.domain.model.ContentType
import com.tioflix.app.domain.model.HomeCatalog
import com.tioflix.app.domain.model.SeriesEpisode
import com.tioflix.app.domain.model.SeriesSeason
import com.tioflix.app.domain.repository.CatalogRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseCatalogRepository @Inject constructor(
    private val postgrest: Postgrest
) : CatalogRepository {

    override suspend fun getHomeCatalog(): Result<HomeCatalog> = runCatching {
        val categories = postgrest["categories"]
            .select(
                columns = Columns.raw(
                    """
                    id,
                    slug,
                    name,
                    sort_order,
                    content_categories (
                        sort_order,
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
                    )
                    """.trimIndent()
                )
            )
            .decodeList<CategoryDto>()
            .sortedBy { it.sortOrder }
            .map { category ->
                ContentCategory(
                    id = category.id,
                    slug = category.slug,
                    name = category.name,
                    sortOrder = category.sortOrder,
                    items = category.contentCategories
                        .sortedBy { it.sortOrder }
                        .map { it.content.toDomain() }
                )
            }

        val featured = categories
            .asSequence()
            .flatMap { it.items.asSequence() }
            .firstOrNull { it.isFeatured }

        HomeCatalog(featured = featured, categories = categories)
    }

    override suspend fun getContent(contentId: String): Result<ContentItem> = runCatching {
        postgrest["content"]
            .select(
                columns = Columns.raw(
                    """
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
                    """.trimIndent()
                )
            ) {
                eq("id", contentId)
            }
            .decodeSingle<ContentDto>()
            .toDomain()
    }

    override suspend fun getSeriesSeasons(contentId: String): Result<List<SeriesSeason>> = runCatching {
        postgrest["series_seasons"]
            .select(
                columns = Columns.raw(
                    """
                    id,
                    content_id,
                    season_number,
                    title,
                    description,
                    poster_url,
                    series_episodes (
                        id,
                        season_id,
                        episode_number,
                        title,
                        description,
                        thumbnail_url,
                        duration_minutes
                    )
                    """.trimIndent()
                )
            ) {
                eq("content_id", contentId)
            }
            .decodeList<SeriesSeasonDto>()
            .sortedBy { it.seasonNumber }
            .map { season ->
                SeriesSeason(
                    id = season.id,
                    contentId = season.contentId,
                    seasonNumber = season.seasonNumber,
                    title = season.title,
                    description = season.description,
                    posterUrl = season.posterUrl,
                    episodes = season.episodes
                        .sortedBy { it.episodeNumber }
                        .map { episode ->
                            SeriesEpisode(
                                id = episode.id,
                                seasonId = episode.seasonId,
                                episodeNumber = episode.episodeNumber,
                                title = episode.title,
                                description = episode.description,
                                thumbnailUrl = episode.thumbnailUrl,
                                durationMinutes = episode.durationMinutes
                            )
                        }
                )
            }
    }

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
