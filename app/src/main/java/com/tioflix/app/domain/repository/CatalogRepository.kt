package com.tioflix.app.domain.repository

import com.tioflix.app.domain.model.HomeCatalog
import com.tioflix.app.domain.model.SeriesSeason

interface CatalogRepository {
    suspend fun getHomeCatalog(): Result<HomeCatalog>
    suspend fun getSeriesSeasons(contentId: String): Result<List<SeriesSeason>>
}
