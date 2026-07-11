package com.tioflix.app.data.catalog

import com.tioflix.app.domain.model.HomeCatalog
import com.tioflix.app.domain.model.Movie
import com.tioflix.app.domain.model.MovieCategory
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
                    movie_categories (
                        sort_order,
                        movies (
                            id,
                            title,
                            description,
                            poster_url,
                            backdrop_url,
                            release_year,
                            duration_minutes,
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
                MovieCategory(
                    id = category.id,
                    slug = category.slug,
                    name = category.name,
                    sortOrder = category.sortOrder,
                    movies = category.movieCategories
                        .sortedBy { it.sortOrder }
                        .map { it.movies.toDomain() }
                )
            }

        val featured = categories
            .asSequence()
            .flatMap { it.movies.asSequence() }
            .firstOrNull { it.isFeatured }

        HomeCatalog(featured = featured, categories = categories)
    }

    private fun MovieDto.toDomain() = Movie(
        id = id,
        title = title,
        description = description,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseYear = releaseYear,
        durationMinutes = durationMinutes,
        maturityRating = maturityRating,
        language = language,
        isFeatured = isFeatured
    )
}
