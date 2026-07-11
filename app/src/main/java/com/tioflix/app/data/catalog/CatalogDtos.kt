package com.tioflix.app.data.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContentDto(
    val id: String,
    @SerialName("content_type") val contentType: String,
    val title: String,
    val description: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("backdrop_url") val backdropUrl: String? = null,
    @SerialName("release_year") val releaseYear: Int? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("total_seasons") val totalSeasons: Int? = null,
    @SerialName("maturity_rating") val maturityRating: String? = null,
    val language: String? = null,
    @SerialName("is_featured") val isFeatured: Boolean = false
)

@Serializable
data class ContentCategoryLinkDto(
    @SerialName("sort_order") val sortOrder: Int = 0,
    val content: ContentDto
)

@Serializable
data class CategoryDto(
    val id: Long,
    val slug: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("content_categories") val contentCategories: List<ContentCategoryLinkDto> = emptyList()
)

@Serializable
data class SeriesEpisodeDto(
    val id: String,
    @SerialName("season_id") val seasonId: String,
    @SerialName("episode_number") val episodeNumber: Int,
    val title: String,
    val description: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int
)

@Serializable
data class SeriesSeasonDto(
    val id: String,
    @SerialName("content_id") val contentId: String,
    @SerialName("season_number") val seasonNumber: Int,
    val title: String? = null,
    val description: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("series_episodes") val episodes: List<SeriesEpisodeDto> = emptyList()
)
