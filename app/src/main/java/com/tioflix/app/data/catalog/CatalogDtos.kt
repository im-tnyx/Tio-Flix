package com.tioflix.app.data.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("backdrop_url") val backdropUrl: String? = null,
    @SerialName("release_year") val releaseYear: Int? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("maturity_rating") val maturityRating: String? = null,
    val language: String? = null,
    @SerialName("is_featured") val isFeatured: Boolean = false
)

@Serializable
data class MovieCategoryLinkDto(
    @SerialName("sort_order") val sortOrder: Int = 0,
    val movies: MovieDto
)

@Serializable
data class CategoryDto(
    val id: Long,
    val slug: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("movie_categories") val movieCategories: List<MovieCategoryLinkDto> = emptyList()
)
