package com.tioflix.app.domain.repository

import com.tioflix.app.domain.model.HomeCatalog

interface CatalogRepository {
    suspend fun getHomeCatalog(): Result<HomeCatalog>
}
